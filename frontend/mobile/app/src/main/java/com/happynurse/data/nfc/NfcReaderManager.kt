package com.happynurse.data.nfc

import android.app.Activity
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcReaderManager @Inject constructor() {

    fun enable(activity: Activity, onTagRead: (Tag) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V
        adapter.enableReaderMode(activity, { tag -> onTagRead(tag) }, flags, null)
    }

    fun disable(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    }

    /** NDEF URI 레코드 (예: https://.../nfc/redirect?token=ABC) 의 token 쿼리파라미터 추출. */
    fun parsePatientToken(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage ?: return null
            val record = msg.records.firstOrNull() ?: return null
            val uri: Uri = record.toUri() ?: return null
            uri.getQueryParameter("token")
        } catch (_: Exception) {
            null
        } finally {
            runCatching { ndef.close() }
        }
    }
}
