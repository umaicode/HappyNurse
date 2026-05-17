/**
 * 환자 음성 호출용 MediaRecorder 래퍼 훅.
 *
 * - 60초 자동 정지 (MAX_DURATION_MS)
 * - audio/webm;codecs=opus 우선, 미지원(iOS Safari 등) 시 audio/mp4 폴백
 * - 권한 거부·녹음 실패는 state="error" + error 메시지로 노출 (호출자가 토스트 처리)
 */
import { useCallback, useRef, useState } from "react";

type RecorderState = "idle" | "recording" | "processing" | "error";

const MAX_DURATION_MS = 60_000;

export function useVoiceRecorder() {
  const [state, setState] = useState<RecorderState>("idle");
  const [error, setError] = useState<string | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const timerRef = useRef<number | null>(null);

  const start = useCallback(async () => {
    setError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      chunksRef.current = [];
      const mime = MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
        ? "audio/webm;codecs=opus"
        : "audio/mp4";
      const rec = new MediaRecorder(stream, { mimeType: mime });
      rec.ondataavailable = (event) => {
        if (event.data.size > 0) chunksRef.current.push(event.data);
      };
      recorderRef.current = rec;
      rec.start();
      setState("recording");
      timerRef.current = window.setTimeout(() => {
        if (recorderRef.current?.state === "recording") {
          recorderRef.current.stop();
        }
      }, MAX_DURATION_MS);
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "마이크 권한이 거부되었습니다.";
      setError(message);
      setState("error");
    }
  }, []);

  const stop = useCallback((): Promise<Blob | null> => {
    return new Promise((resolve) => {
      const rec = recorderRef.current;
      if (!rec || rec.state !== "recording") {
        resolve(null);
        return;
      }
      rec.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: rec.mimeType });
        streamRef.current?.getTracks().forEach((track) => track.stop());
        if (timerRef.current) {
          clearTimeout(timerRef.current);
          timerRef.current = null;
        }
        chunksRef.current = [];
        streamRef.current = null;
        recorderRef.current = null;
        setState("processing");
        resolve(blob);
      };
      rec.stop();
    });
  }, []);

  const reset = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    chunksRef.current = [];
    streamRef.current = null;
    recorderRef.current = null;
    setState("idle");
    setError(null);
  }, []);

  return { state, error, start, stop, reset };
}
