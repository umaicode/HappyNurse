// 알람 TTS 발화기 — 알람이 울릴 때 텍스트를 한국어 음성으로 읽어준다.
// WearApplication.onCreate() 에서 warmUp() 으로 미리 엔진을 로드해두면, 알람 시점에 즉시 speak() 가능.
package com.happynurse.wear.alarm

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmTtsSpeaker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    // 앱 시작 시 1회 호출. TextToSpeech 초기화는 0.5~2초 소요되므로 알람 시점에 만들면 늦는다.
    fun warmUp() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TTS init 실패 status=$status")
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            // 알람 카테고리로 출력 — 워치 무음 모드에서도 가능한 한 들리도록
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            val result = engine.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "한국어 TTS 음성 데이터 없음 — 워치 설정에서 다운로드 필요")
                return@TextToSpeech
            }
            ready = true
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        val engine = tts
        if (engine == null || !ready) {
            Log.w(TAG, "TTS 준비 안 됨 — speak skip (text=$text)")
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alarm-${System.currentTimeMillis()}")
    }

    private companion object {
        const val TAG = "AlarmTtsSpeaker"
    }
}
