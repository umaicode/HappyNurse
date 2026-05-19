// SelfReportAlarmActivity — 환자요청(자가보고) 풀스크린 알람. priority=CRITICAL/HIGH 일 때만 띄움.
// SttAlarmActivity 와 동일한 패턴: ringtone(짧게) → TTS, 진동 병행, 사운드 모드 존중.
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
import androidx.lifecycle.lifecycleScope
import com.happynurse.wear.presentation.screens.alarm.SelfReportAlarmScreen
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelfReportAlarmActivity : ComponentActivity() {

    @Inject lateinit var ttsSpeaker: AlarmTtsSpeaker

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var originalAlarmVolume: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        )

        val patient = intent.getStringExtra(EXTRA_PATIENT).orEmpty()
        val room = intent.getStringExtra(EXTRA_ROOM).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val priority = intent.getStringExtra(EXTRA_PRIORITY).orEmpty()

        val ringerMode = am().ringerMode
        val playSound = ringerMode == AudioManager.RINGER_MODE_NORMAL
        val playVibration = ringerMode == AudioManager.RINGER_MODE_VIBRATE

        if (playSound) {
            boostAlarmVolume()
            startAlarmRingtone()
        }
        if (playVibration) {
            startAlarmVibration()
        }
        runCatching { nm().cancelAll() }

        if (playSound) {
            val spoken = listOf(body, patient, room).firstOrNull { it.isNotBlank() }
                ?: "환자 요청이 도착했습니다"
            lifecycleScope.launch {
                delay(RINGTONE_DURATION_MS)
                runCatching { ringtone?.stop() }
                ringtone = null
                delay(150L)
                ttsSpeaker.speak(spoken)
            }
        }

        setContent {
            HappyNurseWearTheme {
                SelfReportAlarmScreen(
                    patientName = patient,
                    roomLocation = room,
                    body = body.ifBlank { "환자 요청이 도착했습니다" },
                    priority = priority,
                    onDismiss = {
                        stopAlarmFeedback()
                        runCatching { nm().cancelAll() }
                        finish()
                    },
                )
            }
        }
    }

    private fun startAlarmRingtone() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = false
                }
                play()
            }
        }
    }

    private fun startAlarmVibration() {
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

    private fun boostAlarmVolume() {
        runCatching {
            val audio = am()
            originalAlarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        }
    }

    private fun restoreAlarmVolume() {
        if (originalAlarmVolume < 0) return
        runCatching {
            am().setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0)
        }
        originalAlarmVolume = -1
    }

    private fun stopAlarmFeedback() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    override fun onDestroy() {
        stopAlarmFeedback()
        restoreAlarmVolume()
        super.onDestroy()
    }

    private fun nm() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private fun am() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    companion object {
        const val EXTRA_PATIENT = "extra.patient"
        const val EXTRA_ROOM = "extra.room"
        const val EXTRA_BODY = "extra.body"
        const val EXTRA_PRIORITY = "extra.priority"
        private const val RINGTONE_DURATION_MS = 500L
    }
}
