package com.happynurse.wear.data.timer

import android.os.CountDownTimer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CountDownTimerManager @Inject constructor() {

    private var timer: CountDownTimer? = null

    fun start(durationMillis: Long, onTick: (remainingMillis: Long) -> Unit, onFinish: () -> Unit) {
        cancel()
        timer = object : CountDownTimer(durationMillis, TICK_INTERVAL_MILLIS) {
            override fun onTick(millisUntilFinished: Long) = onTick(millisUntilFinished)
            override fun onFinish() = onFinish()
        }.also { it.start() }
    }

    fun cancel() {
        timer?.cancel()
        timer = null
    }

    companion object {
        private const val TICK_INTERVAL_MILLIS = 1000L
    }
}
