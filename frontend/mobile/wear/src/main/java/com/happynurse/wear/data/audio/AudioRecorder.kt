// MediaRecorder 기반 음성 녹음 — AAC 인코딩 m4a 파일을 cacheDir 에 생성하고 절대 경로 File 을 반환한다.
// 음성STT_022/023 연관 (시작/종료, 노이즈 캔슬링은 서버 측에서 처리).
package com.happynurse.wear.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start() {
        stopQuietly()
        val file = File(context.cacheDir, "stt_${System.currentTimeMillis()}.m4a")
        outputFile = file
        mediaRecorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(SAMPLE_RATE_HZ)
            setAudioEncodingBitRate(BITRATE_BPS)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    fun stop(): File? {
        val recorder = mediaRecorder ?: return null
        return runCatching {
            recorder.stop()
            recorder.release()
            outputFile
        }.also {
            mediaRecorder = null
        }.getOrNull()
    }

    fun cancel() {
        stopQuietly()
        outputFile?.delete()
        outputFile = null
    }

    // 마지막 호출 이후 최대 amplitude. 녹음 중이 아니면 0.
    fun currentAmplitude(): Int = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    private fun stopQuietly() {
        mediaRecorder?.runCatching {
            stop()
            release()
        }
        mediaRecorder = null
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val BITRATE_BPS = 64_000
    }
}
