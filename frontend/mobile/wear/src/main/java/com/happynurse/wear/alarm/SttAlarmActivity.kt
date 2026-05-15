// SttAlarmActivity — STT 타이머 풀스크린 알람 호스트.
// 짧은 ringtone(한 번) → 끝나면 TTS 발화 직렬화. 진동은 동시 진행.
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
import com.happynurse.wear.presentation.screens.alarm.SttAlarmScreen
import com.happynurse.wear.presentation.theme.HappyNurseWearTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SttAlarmActivity : ComponentActivity() {

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

        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val patient = intent.getStringExtra(EXTRA_PATIENT).orEmpty()
        val roomBedTime = intent.getStringExtra(EXTRA_ROOM_BED_TIME).orEmpty()

        // 워치의 사운드 모드를 존중한다.
        //   NORMAL  → ringtone + TTS (진동 없음)
        //   VIBRATE → 진동만
        //   SILENT  → 화면만
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
        // 풀스크린 인텐트가 도착했으면 trigger 한 notification 도 같이 정리 (헤드업 잔상 방지)
        runCatching { nm().cancelAll() }

        // ringtone 짧게 (RINGTONE_DURATION_MS) 울린 뒤 멈추고 TTS 발화. 동시 출력 방지.
        if (playSound) {
            val spoken = listOf(content, patient, roomBedTime).firstOrNull { it.isNotBlank() }
                ?: "알람 시각이 되었습니다"
            lifecycleScope.launch {
                delay(RINGTONE_DURATION_MS)
                runCatching { ringtone?.stop() }
                ringtone = null
                // ringtone audio 가 완전히 비워질 짧은 여유 후 TTS.
                delay(150L)
                ttsSpeaker.speak(spoken)
            }
        }

        setContent {
            HappyNurseWearTheme {
                SttAlarmScreen(
                    contentSummary = content.ifBlank { "알람 시각이 되었습니다" },
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

    // 사용자가 워치 알람 볼륨을 낮춰둔 경우에도 들리도록 일시 최대화. onDestroy 에서 복원.
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
        const val EXTRA_CONTENT = "extra.content"
        const val EXTRA_ROOM_BED_TIME = "extra.room_bed_time"
        // ringtone 을 짧게 (~0.5초) 울린 뒤 TTS 로 넘긴다.
        private const val RINGTONE_DURATION_MS = 500L
    }
}
