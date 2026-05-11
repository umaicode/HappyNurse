// AI 서버 STT 음성 인식용 Retrofit 인터페이스. multipart 로 audio 파일 한 개를 업로드한다.
package com.happynurse.wear.data.remote.api

import com.happynurse.wear.data.remote.model.SttRecognizeResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SttRecognitionApi {
    @Multipart
    @POST("api/stt/recognize")
    suspend fun recognize(
        @Part audio: MultipartBody.Part,
    ): SttRecognizeResponse
}
