// 센서 기반 제스처 감지 — 손목 더블 스냅(빠른 좌→우→좌→우 회전 2회) 을 자이로 데이터로 인식.
// GestureService 가 start()/stop() 으로 라이프사이클을 제어한다.
package com.happynurse.wear.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) : SensorEventListener {

    enum class Gesture { WRIST_DOUBLE_SNAP }

    private val _events = MutableSharedFlow<Gesture>(extraBufferCapacity = 4)
    val events: SharedFlow<Gesture> = _events.asSharedFlow()

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    @Volatile private var running = false

    // 스냅 검출 상태
    private var lastSnapTimestampMs: Long = 0L
    private var lastSnapSign: Int = 0          // +1 / -1 / 0
    private var lastEmitTimestampMs: Long = 0L

    fun start() {
        if (running) return
        val sm = sensorManager ?: run {
            Log.w(TAG, "SensorManager unavailable")
            return
        }
        val gyro = gyroscope ?: run {
            Log.w(TAG, "Gyroscope not available on this device")
            return
        }
        sm.registerListener(this, gyro, SAMPLING_PERIOD_US)
        running = true
        Log.d(TAG, "GestureDetector started")
    }

    fun stop() {
        if (!running) return
        sensorManager?.unregisterListener(this)
        running = false
        lastSnapTimestampMs = 0L
        lastSnapSign = 0
        Log.d(TAG, "GestureDetector stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        // gyro x = roll (손목 회전축). Galaxy Watch 기준 손목을 빠르게 회전하면 |gx| 가 peak.
        val gx = event.values[0]
        val absGx = abs(gx)
        if (absGx < GYRO_PEAK_THRESHOLD) return

        val nowMs = System.currentTimeMillis()
        if (nowMs - lastEmitTimestampMs < EMIT_COOLDOWN_MS) return

        val sign = if (gx >= 0f) 1 else -1
        val sinceLastSnap = nowMs - lastSnapTimestampMs

        when {
            // 첫 스냅 or 너무 오래 지난 경우 → 첫 스냅으로 기록
            lastSnapTimestampMs == 0L || sinceLastSnap > MAX_GAP_MS -> {
                lastSnapTimestampMs = nowMs
                lastSnapSign = sign
            }
            // 너무 빨라 같은 peak 의 연속 샘플 — 무시
            sinceLastSnap < MIN_GAP_MS -> Unit
            // 두 번째 스냅 — 방향이 반대일 때만 더블 스냅으로 인정 (한 번 비틀고 풀기)
            sign != lastSnapSign -> {
                Log.d(TAG, "WRIST_DOUBLE_SNAP detected gap=${sinceLastSnap}ms")
                _events.tryEmit(Gesture.WRIST_DOUBLE_SNAP)
                lastEmitTimestampMs = nowMs
                lastSnapTimestampMs = 0L
                lastSnapSign = 0
            }
            // 같은 방향 — 재시작
            else -> {
                lastSnapTimestampMs = nowMs
                lastSnapSign = sign
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val TAG = "GestureDetector"

        // SENSOR_DELAY_GAME ≒ 20ms 샘플링
        const val SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_GAME

        // 손목 회전 각속도 임계값 (rad/s). 일반 손짓·시계 보기 동작은 ~2 rad/s, 빠른 스냅은 8+ rad/s.
        const val GYRO_PEAK_THRESHOLD = 8.0f

        // 두 스냅 사이 허용 시간 범위
        const val MIN_GAP_MS = 80L
        const val MAX_GAP_MS = 1200L

        // 이중 발화 방지
        const val EMIT_COOLDOWN_MS = 2_000L
    }
}
