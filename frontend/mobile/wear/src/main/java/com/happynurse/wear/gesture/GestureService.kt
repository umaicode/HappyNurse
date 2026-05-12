// GestureService — 로그인 동안 상시 동작하는 ForegroundService.
// 손목 더블 스냅을 감지하면 WearMainActivity 를 녹음 페이지로 띄운다.
package com.happynurse.wear.gesture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.happynurse.wear.WearMainActivity
import com.happynurse.wear.data.sensor.GestureDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GestureService : Service() {

    @Inject lateinit var gestureDetector: GestureDetector

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null
    private var lastFireMs = 0L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        ensureChannel(this)
        startInForeground()
        gestureDetector.start()
        collectJob = scope.launch {
            gestureDetector.events.collectLatest { event ->
                when (event) {
                    GestureDetector.Gesture.WRIST_DOUBLE_SNAP -> handleGesture()
                }
            }
        }
    }

    private fun startInForeground() {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("HappyNurse")
            .setContentText("녹음 단축 제스처 활성화 중")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun handleGesture() {
        val now = System.currentTimeMillis()
        if (now - lastFireMs < FIRE_COOLDOWN_MS) return
        lastFireMs = now
        Log.d(TAG, "WRIST_DOUBLE_SNAP → posting full-screen intent for record screen")
        vibrateFeedback()
        ensureLaunchChannel(this)

        val activityIntent = Intent(this, WearMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_RECORD, true)
        }
        val pi = PendingIntent.getActivity(
            this,
            LAUNCH_REQUEST_CODE,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, LAUNCH_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("녹음 시작")
            .setContentText("손목 제스처 감지")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        getSystemService<NotificationManager>()?.notify(LAUNCH_NOTIF_ID, notif)
    }

    private fun vibrateFeedback() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        gestureDetector.stop()
        collectJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OPEN_RECORD = "extra.open_record"

        private const val TAG = "GestureService"
        private const val CHANNEL_ID = "happynurse_gesture"
        private const val CHANNEL_NAME = "녹음 단축 제스처"
        private const val NOTIF_ID = 1042
        private const val FIRE_COOLDOWN_MS = 3_000L

        // 풀스크린 인텐트 — 백그라운드에서 Activity 를 띄우기 위한 채널 (IMPORTANCE_HIGH 필요)
        private const val LAUNCH_CHANNEL_ID = "happynurse_gesture_launch"
        private const val LAUNCH_CHANNEL_NAME = "녹음 단축 실행"
        private const val LAUNCH_NOTIF_ID = 1043
        private const val LAUNCH_REQUEST_CODE = 4242

        private fun ensureLaunchChannel(context: Context) {
            val nm = context.getSystemService<NotificationManager>() ?: return
            if (nm.getNotificationChannel(LAUNCH_CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                LAUNCH_CHANNEL_ID, LAUNCH_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "손목 제스처로 녹음 화면 즉시 실행"
                setBypassDnd(true)
            }
            nm.createNotificationChannel(channel)
        }

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, GestureService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GestureService::class.java))
        }

        private fun ensureChannel(context: Context) {
            val nm = context.getSystemService<NotificationManager>() ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "워치 손목 더블 스냅으로 녹음 시작 단축"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
