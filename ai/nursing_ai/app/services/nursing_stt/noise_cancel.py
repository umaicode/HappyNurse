"""STT 전송 전 노이즈 캔슬링 전략.

STT_NOISE_CANCEL 환경변수로 모드를 선택한다:
  - off : 아무것도 하지 않음 (기존 동작과 동일)
  - dsp : noisereduce 기반 스펙트럼 게이팅 (기본값)
  - ml  : DeepFilterNet (별도 PR에서 활성화 예정)
"""

import os
import time
from typing import Protocol

import numpy as np


class NoiseCanceller(Protocol):
    def apply(self, pcm: np.ndarray, sr: int) -> np.ndarray: ...


class NoOpNoiseCanceller:
    name = "off"

    def apply(self, pcm: np.ndarray, sr: int) -> np.ndarray:
        return pcm


class SpectralGateNoiseCanceller:
    """noisereduce 기반 비정상(non-stationary) 스펙트럼 게이팅.

    병원 환경의 transient(발걸음·문 소리)와 주기적 톤(모니터·펌프 알람)에
    적응하도록 파라미터를 잡았다. 경쟁 화자에는 효과 미미함.
    """

    name = "dsp"

    def __init__(
        self,
        prop_decrease: float = 0.85,
        n_fft: int = 1024,
        time_constant_s: float = 2.0,
        freq_mask_smooth_hz: int = 500,
        time_mask_smooth_ms: int = 50,
    ):
        import noisereduce as nr  # 지연 임포트로 off 모드일 땐 미설치여도 OK

        self._nr = nr
        self.prop_decrease = prop_decrease
        self.n_fft = n_fft
        self.time_constant_s = time_constant_s
        self.freq_mask_smooth_hz = freq_mask_smooth_hz
        self.time_mask_smooth_ms = time_mask_smooth_ms

    def apply(self, pcm: np.ndarray, sr: int) -> np.ndarray:
        try:
            t0 = time.perf_counter()
            cleaned = self._nr.reduce_noise(
                y=pcm,
                sr=sr,
                stationary=False,
                prop_decrease=self.prop_decrease,
                n_fft=self.n_fft,
                time_constant_s=self.time_constant_s,
                freq_mask_smooth_hz=self.freq_mask_smooth_hz,
                time_mask_smooth_ms=self.time_mask_smooth_ms,
            )
            elapsed_ms = (time.perf_counter() - t0) * 1000.0
            print(
                f"[NC dsp] {len(pcm) / sr:.2f}s 클립 처리: {elapsed_ms:.1f}ms"
            )
            return cleaned.astype(np.float32, copy=False)
        except Exception as e:
            print(f"[NC dsp] 실패, 원본 PCM 유지: {e}")
            return pcm


class DeepFilterNetNoiseCanceller:
    """2단계 ML 기반 NC. 본 PR에서는 placeholder."""

    name = "ml"

    def __init__(self):
        raise NotImplementedError(
            "DeepFilterNet 모드는 후속 PR에서 활성화 예정입니다. "
            "STT_NOISE_CANCEL=dsp 또는 off 를 사용하세요."
        )

    def apply(self, pcm: np.ndarray, sr: int) -> np.ndarray:  # pragma: no cover
        raise NotImplementedError


_INSTANCE: NoiseCanceller | None = None


def get_noise_canceller() -> NoiseCanceller:
    """프로세스 라이프타임 동안 1회만 인스턴스화하는 싱글턴 팩토리."""
    global _INSTANCE
    if _INSTANCE is not None:
        return _INSTANCE

    mode = os.getenv("STT_NOISE_CANCEL", "dsp").strip().lower()
    if mode == "off":
        _INSTANCE = NoOpNoiseCanceller()
    elif mode == "ml":
        _INSTANCE = DeepFilterNetNoiseCanceller()
    elif mode == "dsp":
        _INSTANCE = SpectralGateNoiseCanceller()
    else:
        print(f"[NC] 알 수 없는 STT_NOISE_CANCEL='{mode}', off로 폴백")
        _INSTANCE = NoOpNoiseCanceller()

    print(f"[NC] 노이즈 캔슬링 모드: {_INSTANCE.name}")
    return _INSTANCE
