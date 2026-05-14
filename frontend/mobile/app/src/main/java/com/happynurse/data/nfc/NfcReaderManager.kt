// NFC reader-mode 매니저 — Activity 단위로 NFC 어댑터 enable/disable + NDEF 메시지 콜백
package com.happynurse.data.nfc

import android.app.Activity
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcReaderManager @Inject constructor() {

    // owner tracking — 화면 전환 시 새 ViewModel 의 enable 이 들어온 뒤
    // 옛 ViewModel 의 dispose-disable 이 reader-mode 를 끄지 못하게 한다.
    @Volatile private var currentOwner: Any? = null

    fun enable(activity: Activity, owner: Any, onTagRead: (Tag) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null) {
            Log.w("NfcReaderManager", "enable: NfcAdapter is null (NFC unsupported?)")
            return
        }
        currentOwner = owner
        // FLAG_READER_SKIP_NDEF_CHECK 필수 — 약물 칩(NDEF empty) 태깅 시 OS dispatch 로 fall-through 방지.
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(activity, { tag ->
            val uid = tag.id.joinToString(":") { "%02X".format(it) }
            Log.d("NfcReaderManager", "tag detected: uid=$uid techList=${tag.techList.joinToString()}")
            onTagRead(tag)
        }, flags, null)
        Log.d("NfcReaderManager", "enable: reader-mode ENABLED owner=${owner.javaClass.simpleName}")
    }

    fun disable(activity: Activity, owner: Any) {
        if (currentOwner !== owner) {
            Log.d("NfcReaderManager", "disable SKIPPED — currentOwner=${currentOwner?.javaClass?.simpleName} requestor=${owner.javaClass.simpleName}")
            return
        }
        currentOwner = null
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
        Log.d("NfcReaderManager", "disable: reader-mode DISABLED owner=${owner.javaClass.simpleName}")
    }

    /** NDEF URI 레코드 (예: https://.../nfc/redirect?token=ABC) 의 token 쿼리파라미터 추출.
     *  cachedNdefMessage 가 있으면 connect 없이 즉시 사용 — reader mode 가 미리 채워두므로 첫 시도 성공률 ↑. */
    fun parsePatientToken(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        val msg = ndef.cachedNdefMessage ?: try {
            ndef.connect()
            ndef.ndefMessage
        } catch (_: Exception) {
            null
        } finally {
            runCatching { ndef.close() }
        } ?: return null
        val record = msg.records.firstOrNull() ?: return null
        val uri: Uri = record.toUri() ?: return null
        return uri.getQueryParameter("token")
    }
}
