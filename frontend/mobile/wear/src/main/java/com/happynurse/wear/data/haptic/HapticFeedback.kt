package com.happynurse.wear.data.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticFeedback @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun success() = vibrate(SUCCESS_PATTERN)
    fun timerEnd() = vibrate(TIMER_END_PATTERN)
    fun error() = vibrate(ERROR_PATTERN)

    private fun vibrate(pattern: LongArray) {
        val vibratorRef = vibrator ?: return
        val effect = VibrationEffect.createWaveform(pattern, NO_REPEAT)
        vibratorRef.vibrate(effect)
    }

    companion object {
        private const val NO_REPEAT = -1
        private val SUCCESS_PATTERN = longArrayOf(0, 80)
        private val TIMER_END_PATTERN = longArrayOf(0, 200, 100, 200, 100, 400)
        private val ERROR_PATTERN = longArrayOf(0, 100, 50, 100, 50, 100)
    }
}
