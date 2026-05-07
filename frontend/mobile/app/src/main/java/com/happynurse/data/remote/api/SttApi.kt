// STT (음성인식) Retrofit 인터페이스 — POST /api/stt/recognize (multipart audio + patient/encounter query)
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.SttRecognizeResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface SttApi {
    // 음성 파일 업로드 → CLOVA Speech STT 변환 + 의료용어 자동 교정 + nursing_record DB 저장.
    // ApiResponse wrapper 없이 SttRecognizeResponse 자체를 반환 (FastAPI).
    @Multipart
    @POST("api/stt/recognize")
    suspend fun recognize(
        @Part audio: MultipartBody.Part,
        @Query("patient_id") patientId: Long?,
        @Query("encounter_id") encounterId: Long?,
    ): Response<SttRecognizeResponse>
}
