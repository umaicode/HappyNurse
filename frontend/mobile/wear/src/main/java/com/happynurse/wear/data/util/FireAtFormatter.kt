// fireAtEpochMillis 를 사용자에게 보여줄 자연어 시간 표현으로 바꾼다.
// 1시간 미만이면 "30분 후" / 1시간 이상이면 "14:55" 시각 형식으로 표시한다.
package com.happynurse.wear.data.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FireAtFormatter {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun format(fireAtEpochMillis: Long, now: Instant = Instant.now()): String {
        val target = Instant.ofEpochMilli(fireAtEpochMillis)
        val secondsLeft = Duration.between(now, target).seconds
        return when {
            secondsLeft <= 0L -> "곧"
            secondsLeft < ONE_HOUR_SEC -> {
                val minutes = ((secondsLeft + 30) / 60).toInt().coerceAtLeast(1)
                "${minutes}분 후"
            }
            else -> target.atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)
        }
    }

    private const val ONE_HOUR_SEC = 60L * 60L
}
