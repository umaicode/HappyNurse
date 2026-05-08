// STT Repository — multipart audio 업로드 → SttRecognizeResponse 반환
package com.happynurse.data.repository

import com.happynurse.data.remote.api.SttApi
import com.happynurse.data.remote.model.SttRecognizeResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SttRepository @Inject constructor(
    private val api: SttApi,
) {
    suspend fun recognize(
        audioFile: File,
        patientId: Long?,
        encounterId: Long?,
    ): Result<SttRecognizeResponse> = runCatching {
        // m4a / wav / mp3 / webm 모두 지원. 파일 확장자로 mime 추정.
        val mime = when (audioFile.extension.lowercase()) {
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "webm" -> "audio/webm"
            else -> "application/octet-stream"
        }
        val body = audioFile.asRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("audio", audioFile.name, body)
        val res = api.recognize(part, patientId, encounterId)
        val data = res.body()
        if (res.isSuccessful && data != null && data.success) data
        else throw Exception("STT 변환 실패")
    }
}
