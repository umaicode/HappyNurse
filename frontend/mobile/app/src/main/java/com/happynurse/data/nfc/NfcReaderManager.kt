package com.happynurse.data.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcReaderManager @Inject constructor() {

    fun enable(activity: Activity, onTagRead: (Tag) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(activity, { tag -> onTagRead(tag) }, flags, null)
    }

    fun disable(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    }

    // TODO 백엔드/보안 팀 결정 후 실제 파싱/AES 복호화 구현
    fun parsePatientToken(@Suppress("UNUSED_PARAMETER") tag: Tag): String? = null
}
