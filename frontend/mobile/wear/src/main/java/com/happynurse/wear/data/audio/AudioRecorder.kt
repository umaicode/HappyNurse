// MediaRecorder 래퍼 — MP3 음성 녹음 startRecording/stopRecording 제공
package com.happynurse.wear.data.audio

import android.content.Context
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// 음성STT_022: 녹음 시작/종료
// 음성STT_023: 노이즈 캔슬링 적용 후 음성 파일 반환
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String = ""

    fun startRecording() {
        outputFilePath = "${context.cacheDir}/recording_${System.currentTimeMillis()}.mp3"
        mediaRecorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFilePath)
            prepare()
            start()
        }
    }

    fun stopRecording(): String {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        return outputFilePath
    }

    // 음성STT_023: 병동 노이즈 캔슬링
    // TODO: 온디바이스 노이즈 필터 DSP 적용
    fun applyNoiseCancellation(filePath: String): String {
        return filePath
    }
}
