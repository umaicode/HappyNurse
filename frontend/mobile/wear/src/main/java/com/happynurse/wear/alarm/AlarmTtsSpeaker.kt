// 알람 TTS 발화기 — 알람이 울릴 때 텍스트를 한국어 음성으로 읽어준다.
// WearApplication.onCreate() 에서 warmUp() 으로 미리 엔진을 로드해두면, 알람 시점에 즉시 speak() 가능.
// 단, 알람으로 인해 프로세스가 cold-start 된 경우 init 콜백이 onReceive 보다 늦게 끝나므로
// pending 큐에 보관했다가 초기화 완료 시점에 자동 발화한다.
package com.happynurse.wear.alarm

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmTtsSpeaker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    // init 완료 전에 들어온 speak 요청을 보관 — 보통 알람 cold-start 시 1건
    private val pending = ConcurrentLinkedQueue<String>()

    // 앱 시작 시 1회 호출. TextToSpeech 초기화는 0.5~2초 소요되므로 알람 시점에 만들면 늦다.
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
            // 초기화 전에 들어온 발화 요청 처리
            drainPending()
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        val engine = tts
        if (engine == null) {
            // warmUp 이 아예 호출되지 않았으면 lazy 로 시작 + 큐에 저장
            Log.d(TAG, "TTS 미초기화 — warmUp 트리거 후 큐 저장 (text=$text)")
            pending.offer(text)
            warmUp()
            return
        }
        if (!ready) {
            // 초기화 진행 중 — 큐에 보관, init 콜백에서 발화
            Log.d(TAG, "TTS 초기화 진행 중 — 큐 저장 (text=$text)")
            pending.offer(text)
            return
        }
        speakNow(engine, text)
    }

    private fun drainPending() {
        val engine = tts ?: return
        while (true) {
            val next = pending.poll() ?: break
            speakNow(engine, next)
        }
    }

    private fun speakNow(engine: TextToSpeech, text: String) {
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, "alarm-${System.currentTimeMillis()}")
    }

    private companion object {
        const val TAG = "AlarmTtsSpeaker"
    }
}
