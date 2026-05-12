"""NC 적용 전/후 mel-spectrogram 비교 시각화.

compare_nc CLI 가 import 해서 사용. 프로덕션 코드와는 분리되어 있다.
"""

from __future__ import annotations

import io
from pathlib import Path

import numpy as np
from pydub import AudioSegment

from app.services.nursing_stt.noise_cancel import get_noise_canceller


SR = 16000
N_FFT = 1024
HOP_LENGTH = 256
N_MELS = 80


def _load_normalized_pcm(audio_bytes: bytes, filename: str) -> np.ndarray:
    """clova_stt.convert_to_wav_with_nc 와 동일 경로로 16kHz/mono/float32 PCM 추출."""
    ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else "wav"
    audio = AudioSegment.from_file(io.BytesIO(audio_bytes), format=ext)
    audio = audio.set_frame_rate(SR).set_channels(1).set_sample_width(2)
    samples = np.array(audio.get_array_of_samples(), dtype=np.int16)
    return samples.astype(np.float32) / 32768.0


def _mel_db(pcm: np.ndarray) -> np.ndarray:
    import librosa

    mel = librosa.feature.melspectrogram(
        y=pcm, sr=SR, n_fft=N_FFT, hop_length=HOP_LENGTH, n_mels=N_MELS
    )
    return librosa.power_to_db(mel, ref=np.max)


def _rms_db(pcm: np.ndarray) -> float:
    rms = float(np.sqrt(np.mean(pcm.astype(np.float64) ** 2)))
    if rms <= 0:
        return -120.0
    return 20.0 * float(np.log10(rms))


def render_nc_comparison(
    audio_bytes: bytes, filename: str, out_path: Path
) -> dict:
    """
    원본 PCM 과 NC 적용 PCM 의 mel-spectrogram 을 나란히 그리고 delta panel 까지 추가.
    PNG 저장 후 메트릭 dict 반환.
    """
    import librosa.display
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    pcm_original = _load_normalized_pcm(audio_bytes, filename)
    canceller = get_noise_canceller()
    pcm_clean = canceller.apply(pcm_original.copy(), sr=SR)
    if pcm_clean.shape != pcm_original.shape:
        # noisereduce 가 가끔 짝수 padding 으로 길이 1 더할 수 있음 → 잘라 맞춤
        n = min(pcm_original.shape[0], pcm_clean.shape[0])
        pcm_original = pcm_original[:n]
        pcm_clean = pcm_clean[:n]

    mel_orig = _mel_db(pcm_original)
    mel_clean = _mel_db(pcm_clean)
    mel_delta = mel_clean - mel_orig

    # 가장 크게 깎인 mel-band 의 대략적 주파수 추정 (정성 메트릭)
    band_attenuation = mel_delta.mean(axis=1)
    worst_band_idx = int(np.argmin(band_attenuation))
    mel_freqs = librosa.mel_frequencies(n_mels=N_MELS, fmin=0, fmax=SR / 2)
    worst_band_hz = float(mel_freqs[worst_band_idx])
    peak_attenuation_db = float(band_attenuation.min())

    fig, axes = plt.subplots(3, 1, figsize=(12, 9), sharex=True)
    titles = [
        f"original (NC=off) — {filename}",
        f"after NC ({canceller.name})",
        "delta = NC - original (negative = attenuated)",
    ]
    data = [mel_orig, mel_clean, mel_delta]
    cmaps = ["magma", "magma", "RdBu_r"]

    for ax, title, d, cmap in zip(axes, titles, data, cmaps):
        if cmap == "RdBu_r":
            vmax = float(np.max(np.abs(d)))
            kwargs = {"vmin": -vmax, "vmax": vmax}
        else:
            kwargs = {}
        img = librosa.display.specshow(
            d,
            sr=SR,
            hop_length=HOP_LENGTH,
            x_axis="time",
            y_axis="mel",
            ax=ax,
            cmap=cmap,
            **kwargs,
        )
        ax.set_title(title)
        fig.colorbar(img, ax=ax, format="%+2.0f dB")

    fig.tight_layout()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(out_path, dpi=120)
    plt.close(fig)

    return {
        "duration_s": float(len(pcm_original) / SR),
        "rms_db_original": _rms_db(pcm_original),
        "rms_db_clean": _rms_db(pcm_clean),
        "peak_attenuation_db": peak_attenuation_db,
        "worst_attenuated_band_hz": worst_band_hz,
        "nc_mode": canceller.name,
        "png_path": str(out_path),
    }
