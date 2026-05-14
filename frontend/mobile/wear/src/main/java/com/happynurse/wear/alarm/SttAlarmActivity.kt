// SttAlarmActivity — STT 타이머 풀스크린 알람 호스트. 소리 + 진동 재생, 확인 버튼으로 종료.
package com.happynurse.wear.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.happynurse.wear.presentation.screens.alarm.SttAlarmScreen
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme

class SttAlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )

        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()

        startAlarmFeedback()
        // 풀스크린 인텐트가 도착했으면 trigger 한 notification 도 같이 정리 (헤드업 잔상 방지)
        runCatching { nm().cancelAll() }

        setContent {
            HappyNurseWearTheme {
                SttAlarmScreen(
                    patientName = "",
                    contentSummary = content.ifBlank { "알람 시각이 되었습니다" },
                    roomBedTime = "",
                    onDismiss = {
                        stopAlarmFeedback()
                        runCatching { nm().cancelAll() }
                        finish()
                    },
                )
            }
        }
    }

    private fun startAlarmFeedback() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
        }
        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0L, 500L, 300L, 500L, 300L),
                0,
            )
            vibrator?.vibrate(effect)
        }
    }

    private fun stopAlarmFeedback() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    override fun onDestroy() {
        stopAlarmFeedback()
        super.onDestroy()
    }

    @Suppress("unused")
    private fun nm() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Suppress("unused")
    private fun am() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    companion object {
        const val EXTRA_PATIENT = "extra.patient"
        const val EXTRA_CONTENT = "extra.content"
        const val EXTRA_ROOM_BED_TIME = "extra.room_bed_time"
    }
}
