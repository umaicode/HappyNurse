// NFC URL App Links 진입점 — token 추출 후 MainActivity 로 전달
// 매니페스트 intent-filter (NDEF + VIEW autoVerify) 가 라우팅하므로 별도 action 체크 불필요
package com.happynurse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class NfcEntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = intent?.data?.getQueryParameter("token")
        if (token != null) {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_NFC_TOKEN, token)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(mainIntent)
        }
        finish()
    }

    companion object {
        const val EXTRA_NFC_TOKEN = "nfc_token"
    }
}
