// 센서 기반 제스처 감지 — 자이로 X축(손목 회전) peak 패턴으로 손목 제스처 인식.
// Mode 에 따라 두 패턴 중 하나만 활성화:
//   - DOUBLE_SNAP: 빠른 좌→우→좌→우 회전 2회 (반대 방향 두 peak) — 앱 외부에서 녹음 시작 트리거
//   - SINGLE_SNAP: 한 번 회전 (X축 peak 1회) + 다축 정지 가드 — 녹음 중지/등록 확정 트리거
// GestureService 가 start()/stop() 으로 라이프사이클을 제어하고, setMode() 로 patten 을 전환한다.
package com.happynurse.wear.gesture

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

    enum class Gesture { WRIST_DOUBLE_SNAP, WRIST_SINGLE_SNAP }

    enum class Mode { DOUBLE_SNAP, SINGLE_SNAP }

    private val _events = MutableSharedFlow<Gesture>(extraBufferCapacity = 4)
    val events: SharedFlow<Gesture> = _events.asSharedFlow()

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    @Volatile private var running = false
    @Volatile private var mode: Mode = Mode.DOUBLE_SNAP
    // mode 전환 직후 잔여 회전 차단용. setMode 호출 시 갱신.
    @Volatile private var modeSwitchTimestampMs: Long = 0L

    // DOUBLE_SNAP 검출 상태
    private var lastSnapTimestampMs: Long = 0L
    private var lastSnapSign: Int = 0
    private var lastEmitTimestampMs: Long = 0L
    private var sawQuietSinceLastSnap: Boolean = false

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
        Log.d(TAG, "GestureDetector started (mode=$mode)")
    }

    fun stop() {
        if (!running) return
        sensorManager?.unregisterListener(this)
        running = false
        resetState()
        Log.d(TAG, "GestureDetector stopped")
    }

    fun setMode(newMode: Mode) {
        if (mode == newMode) return
        mode = newMode
        modeSwitchTimestampMs = System.currentTimeMillis()
        resetState()
        Log.d(TAG, "Mode switched → $newMode")
    }

    private fun resetState() {
        lastSnapTimestampMs = 0L
        lastSnapSign = 0
        sawQuietSinceLastSnap = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        when (mode) {
            Mode.DOUBLE_SNAP -> handleDoubleSnap(event)
            Mode.SINGLE_SNAP -> handleSingleSnap(event)
        }
    }

    private fun handleDoubleSnap(event: SensorEvent) {
        val gx = event.values[0]
        val absGx = abs(gx)
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastEmitTimestampMs < EMIT_COOLDOWN_MS) return

        // 두 peak 사이의 정지 구간 추적 — 한 번의 비틀기+복귀 진동(연속 진동)을
        // 두 peak 로 오인식하지 않도록, peak 사이에 |gx|<QUIET_THRESHOLD 인 구간이 있어야 함.
        if (lastSnapTimestampMs != 0L && absGx < BETWEEN_PEAKS_QUIET_THRESHOLD) {
            sawQuietSinceLastSnap = true
        }

        if (absGx < GYRO_PEAK_THRESHOLD) return

        val sign = if (gx >= 0f) 1 else -1
        val sinceLastSnap = nowMs - lastSnapTimestampMs

        when {
            // 첫 peak or 너무 오래 지난 경우 → 첫 peak 로 기록
            lastSnapTimestampMs == 0L || sinceLastSnap > MAX_GAP_MS -> {
                lastSnapTimestampMs = nowMs
                lastSnapSign = sign
                sawQuietSinceLastSnap = false
            }
            // 너무 빨라 같은 비틀기의 진동 — 무시
            sinceLastSnap < MIN_GAP_MS -> Unit
            // 두 peak 사이 정지 구간이 없었으면 한 번의 비틀기+복귀 진동 — 무시
            !sawQuietSinceLastSnap -> Unit
            // 진짜 두 번째 스냅
            sign != lastSnapSign -> {
                Log.d(TAG, "WRIST_DOUBLE_SNAP detected gap=${sinceLastSnap}ms")
                _events.tryEmit(Gesture.WRIST_DOUBLE_SNAP)
                lastEmitTimestampMs = nowMs
                resetState()
            }
            else -> {
                lastSnapTimestampMs = nowMs
                lastSnapSign = sign
                sawQuietSinceLastSnap = false
            }
        }
    }

    private fun handleSingleSnap(event: SensorEvent) {
        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]
        val absGx = abs(gx)
        if (absGx < GYRO_PEAK_THRESHOLD) return

        val nowMs = System.currentTimeMillis()
        // 모드 전환 직후 잔여 회전 차단
        if (nowMs - modeSwitchTimestampMs < MODE_SWITCH_COOLDOWN_MS) return
        // 연속 트리거 차단
        if (nowMs - lastEmitTimestampMs < SINGLE_SNAP_COOLDOWN_MS) return
        // 다축 정지 가드: y/z 축이 같이 돌면 팔 들기/시계 보기 같은 다축 동작 → 무시
        if (abs(gy) + abs(gz) > MULTI_AXIS_QUIET_THRESHOLD) return

        Log.d(TAG, "WRIST_SINGLE_SNAP detected gx=$gx |gy|+|gz|=${abs(gy) + abs(gz)}")
        _events.tryEmit(Gesture.WRIST_SINGLE_SNAP)
        lastEmitTimestampMs = nowMs
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val TAG = "GestureDetector"

        const val SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_GAME

        // 손목 회전 각속도 임계값 (rad/s). DOUBLE_SNAP / SINGLE_SNAP 동일.
        const val GYRO_PEAK_THRESHOLD = 8.0f

        // DOUBLE_SNAP 두 peak 사이 허용 시간 — MIN 을 250ms 로 늘려 한 번 비틀기 + 복귀 진동
        // (보통 80~200ms 안에 발생) 이 더블 스냅으로 오인식되는 것 차단.
        const val MIN_GAP_MS = 250L
        const val MAX_GAP_MS = 1200L

        // 두 peak 사이 정지 구간 임계값 — 이 값 미만으로 자이로가 떨어진 적이 있어야
        // 두 peak 가 별개의 동작으로 인정됨. 한 번의 비틀기는 진동 중 0 근처를 안 지남.
        const val BETWEEN_PEAKS_QUIET_THRESHOLD = 2.0f

        // DOUBLE_SNAP 이중 발화 방지
        const val EMIT_COOLDOWN_MS = 2_000L

        // SINGLE_SNAP 연속 트리거 차단
        const val SINGLE_SNAP_COOLDOWN_MS = 1_500L

        // mode 전환 후 잔여 회전 차단 시간 — RESULT 진입 직후 사용자가 결과 보려고 손목 들 때
        // 의도치 않은 SingleSnap → confirm 자동 트리거 방지를 위해 넉넉히 둠.
        const val MODE_SWITCH_COOLDOWN_MS = 2_500L

        // SINGLE_SNAP 다축 정지 가드 — |gy|+|gz| 가 이 값 이하일 때만 X축 peak 인정
        const val MULTI_AXIS_QUIET_THRESHOLD = 5.0f
    }
}
