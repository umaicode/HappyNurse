// 음성 인식 저장소 — 녹음 파일을 AI 서버에 업로드하여 보정된 텍스트를 받는다.
package com.happynurse.wear.data.repository

import com.happynurse.wear.data.remote.api.SttRecognitionApi
import com.happynurse.wear.data.remote.model.SttRecognizeResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SttRecognitionRepository @Inject constructor(
    private val sttRecognitionApi: SttRecognitionApi,
) {
    suspend fun recognize(audioFile: File): Result<SttRecognizeResponse> = runCatching {
        if (!audioFile.exists()) error("녹음 파일을 찾을 수 없어요")
        val requestBody = audioFile.asRequestBody(AUDIO_MIME.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData(
            name = "audio",
            filename = audioFile.name,
            body = requestBody,
        )
        val response = sttRecognitionApi.recognize(part)
        if (!response.success) error("음성 인식에 실패했어요")
        response
    }

    private companion object {
        const val AUDIO_MIME = "audio/m4a"
    }
}
