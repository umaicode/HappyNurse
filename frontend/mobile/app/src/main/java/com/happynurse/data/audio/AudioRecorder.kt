// 음성 녹음 유틸 — MediaRecorder 로 m4a (AAC) 캡처. cache dir 에 임시 저장.
package com.happynurse.data.audio

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
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    // 녹음 시작 — 권한 (RECORD_AUDIO) 은 호출자가 사전 확인 책임. file 은 cache dir 의 m4a.
    fun start(): File {
        stop()
        val file = File(context.cacheDir, "stt_${System.currentTimeMillis()}.m4a")
        currentFile = file
        @Suppress("DEPRECATION")
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    // 마이크 입력 진폭 — 마지막 호출 이후 PCM 피크 (0..32767). 녹음 중이 아니면 0.
    fun maxAmplitude(): Int = recorder?.maxAmplitude ?: 0

    // 녹음 정지 — 정상 정지 시 file 반환. 실패 / 정지 안 된 경우 null.
    fun stop(): File? {
        val r = recorder ?: return null
        return try {
            r.stop()
            r.release()
            currentFile
        } catch (_: Exception) {
            r.release()
            currentFile?.delete()
            null
        } finally {
            recorder = null
        }
    }
}
