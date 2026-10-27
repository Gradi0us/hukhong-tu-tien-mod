"""Synthesize original ambient sounds for hukhong (client.ambient). numpy + soundfile only.
Run from repo root: python tools/gen_ambient_sounds.py
"""
import os
import numpy as np
import soundfile as sf

SR = 32000
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "hukhong", "sounds", "amb")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(20261027)


def t_axis(sec):
    return np.arange(int(SR * sec)) / SR


def band_noise(n, lo, hi, order=2.0):
    """Circular (seamless) band-limited noise via FFT."""
    spec = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / SR)
    g = np.ones_like(f)
    if lo > 0:
        g = g / (1 + (lo / np.maximum(f, 1e-3)) ** (2 * order))
    g = g / (1 + (f / hi) ** (2 * order))
    x = np.fft.irfft(spec * g, n)
    return x / (np.abs(x).max() + 1e-9)


def circ_add(buf, start, sig):
    n = len(buf)
    idx = (start + np.arange(len(sig))) % n
    np.add.at(buf, idx, sig)


def norm(x, peak=0.8):
    return x / (np.abs(x).max() + 1e-9) * peak


def fade_edges(x, ms=15):
    k = int(SR * ms / 1000)
    x = x.copy()
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    return x


def save(name, x, other=None):
    data = np.stack([x, other], 1) if other is not None else x
    data = np.ascontiguousarray(data.astype(np.float32))
    ch = 1 if data.ndim == 1 else data.shape[1]
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, ch, format="OGG", subtype="VORBIS") as f:
        for i in range(0, len(data), 8192):  # chunked: single huge writes overflow the stack on Windows libsndfile
            f.write(data[i:i + 8192])
    print("wrote", name)


def lfo(t, rate, phase=0.0):
    return 0.5 + 0.5 * np.sin(2 * np.pi * rate * t + phase)


def cyc(f, sec):
    """Snap a frequency so it completes whole cycles within the loop."""
    return max(1, round(f * sec)) / sec


def reverb(x, sec=1.4, mix=0.35):
    n = int(SR * sec)
    ir = band_noise(n, 200, 4500) * np.exp(-np.arange(n) / (SR * sec / 4.5))
    ir[0] = 0
    m = len(x) + n
    wet = np.fft.irfft(np.fft.rfft(x, m) * np.fft.rfft(ir, m), m)[: len(x)]
    return x + mix * wet / (np.abs(wet).max() + 1e-9) * np.abs(x).max()


def void_drone():
    S = 24.0
    t = t_axis(S)
    x = np.zeros_like(t)
    for f, a, r in [(46, 0.9, 0.04), (46.6, 0.8, 0.05), (69, 0.35, 0.07), (92.4, 0.22, 0.03), (138.6, 0.10, 0.09)]:
        f = cyc(f, S)
        x += a * np.sin(2 * np.pi * f * t + 3 * lfo(t, cyc(r, S)) * np.sin(2 * np.pi * cyc(0.25, S) * t)) * (0.6 + 0.4 * lfo(t, cyc(r, S), f))
    air = band_noise(len(t), 90, 700) * (0.35 + 0.65 * lfo(t, cyc(0.083, S), 1.0))
    air2 = band_noise(len(t), 1800, 5200) * 0.05 * lfo(t, cyc(0.125, S), 2.2) ** 3
    L = x * 0.5 + air * 0.55 + air2
    R = x * 0.5 + band_noise(len(t), 90, 700) * 0.5 * (0.35 + 0.65 * lfo(t, cyc(0.083, S), 2.0)) + air2[::-1]
    save("void_drone", norm(L, 0.7), norm(R, 0.7))


def whisper(i):
    sec = 3.2 + 0.5 * i
    t = t_axis(sec)
    env = np.zeros_like(t)
    pos = 0.15
    while pos < sec - 0.8:
        d = rng.uniform(0.25, 0.6)
        env += np.exp(-0.5 * ((t - pos - d / 2) / (d / 3.2)) ** 2) * rng.uniform(0.5, 1.0)
        pos += d + rng.uniform(0.05, 0.3)
    out = np.zeros_like(t)
    for fc0, g in [(500, 1.0), (1500, 0.8), (2600, 0.5)]:
        glide = fc0 * (1 + 0.25 * np.sin(2 * np.pi * rng.uniform(0.4, 1.1) * t + rng.uniform(0, 6)))
        base = band_noise(len(t), fc0 * 0.7, fc0 * 1.4)
        out += g * base * (0.6 + 0.4 * np.sin(2 * np.pi * np.cumsum(glide - fc0) / SR))
    out *= env
    out += 0.15 * band_noise(len(t), 3000, 9000) * env
    out = reverb(out, 1.6, 0.6)
    save(f"void_whisper_{i}", fade_edges(norm(out, 0.55), 40))


def drum(sec, f0=95, f1=42, dec=0.28, noise=0.35):
    t = t_axis(sec)
    ph = 2 * np.pi * np.cumsum(f1 + (f0 - f1) * np.exp(-t / 0.05)) / SR
    body = np.sin(ph) * np.exp(-t / dec)
    thump = band_noise(len(t), 60, 400) * np.exp(-t / 0.03) * noise
    return body + thump


def war_drum():
    S = 16.0
    n = int(SR * S)
    L = np.zeros(n)
    R = np.zeros(n)
    beat = S / 16
    pattern = [(0, 1.0), (1.5, 0.55), (2, 0.8), (4, 1.0), (5.5, 0.5), (6, 0.8), (7, 0.7), (8, 1.0), (9.5, 0.55), (10, 0.8), (12, 1.0), (13, 0.7), (14, 0.9), (14.5, 0.9), (15, 0.6)]
    d = drum(1.6)
    for b, a in pattern:
        s = int(b * beat * SR)
        circ_add(L, s, d * a)
        circ_add(R, s + 40, d * a)
    rum = band_noise(n, 40, 200) * (0.15 + 0.1 * lfo(t_axis(S), cyc(0.125, S)))
    save("war_drum", norm(L + rum, 0.85), norm(R + rum, 0.85))


def chirp(freqs, durs, amp=1.0):
    parts = []
    for f0, f1, d in zip(freqs[:-1], freqs[1:], durs):
        m = int(SR * d)
        f = np.linspace(f0, f1, m)
        ph = 2 * np.pi * np.cumsum(f) / SR
        e = np.sin(np.pi * np.linspace(0, 1, m)) ** 1.5
        parts.append((np.sin(ph) + 0.25 * np.sin(2 * ph)) * e)
    return amp * np.concatenate(parts)


def town_day():
    S = 30.0
    n = int(SR * S)
    t = t_axis(S)
    L = np.zeros(n)
    R = np.zeros(n)
    for _ in range(6):
        fc = rng.uniform(250, 700)
        voice = band_noise(n, fc, fc * 2.2)
        syl = np.clip(np.sin(2 * np.pi * cyc(rng.uniform(2.5, 4.5), S) * t + rng.uniform(0, 6)), 0, 1) ** 2
        gate = lfo(t, cyc(rng.uniform(0.05, 0.15), S), rng.uniform(0, 6)) ** 2
        s = voice * syl * gate * 0.18
        pan = rng.uniform(0.2, 0.8)
        L += s * (1 - pan)
        R += s * pan
    L += band_noise(n, 120, 900) * 0.12
    R += band_noise(n, 120, 900) * 0.12
    for _ in range(26):
        base = rng.uniform(2400, 5200)
        k = int(rng.integers(2, 7))
        fr = [base * rng.uniform(0.8, 1.3) for _ in range(k + 1)]
        du = [rng.uniform(0.04, 0.12) for _ in range(k)]
        c = chirp(fr, du, rng.uniform(0.05, 0.12))
        if rng.random() < 0.6:
            c = np.concatenate([c, np.zeros(int(SR * 0.08)), c * 0.8])
        s = int(rng.uniform(0, S) * SR)
        pan = rng.uniform(0.1, 0.9)
        circ_add(L, s, c * (1 - pan))
        circ_add(R, s, c * pan)
    save("town_day", norm(L, 0.6), norm(R, 0.6))


def port_water():
    S = 20.0
    n = int(SR * S)
    t = t_axis(S)
    L = np.zeros(n)
    R = np.zeros(n)
    for ch, arr in enumerate((L, R)):
        wash = band_noise(n, 300, 2200)
        swell = (0.2 + 0.8 * lfo(t, cyc(0.15, S), 1.3 * ch)) ** 2.0
        arr += wash * swell * 0.6
        arr += band_noise(n, 3000, 7500) * (swell ** 3) * 0.12
    tt = t_axis(1.4)
    for _ in range(14):
        f = float(rng.choice([1318.5, 1568, 1760, 2093, 2349]))
        c = np.sin(2 * np.pi * f * tt) * np.exp(-tt / 0.35) * 0.04
        s = int(rng.uniform(0, S) * SR)
        circ_add(L, s, c)
        circ_add(R, s + 30, c)
    save("port_water", norm(L, 0.55), norm(R, 0.55))


def thunder_far():
    S = 7.0
    n = int(SR * S)
    t = t_axis(S)
    base = band_noise(n, 25, 180, 3)
    rumble = base * (np.exp(-t / 2.4) * (1 - np.exp(-t / 0.5)))
    crack = band_noise(n, 150, 600) * np.exp(-((t - 0.25) / 0.35) ** 2) * 0.5
    sub = np.sin(2 * np.pi * 38 * t) * np.exp(-t / 1.6) * 0.5
    x = (rumble + crack + sub) * (1 + 0.4 * np.sin(2 * np.pi * 3.1 * t) * np.exp(-t / 3))
    save("thunder_far", fade_edges(norm(x, 0.8), 60))


def dust_hum():
    S = 20.0
    t = t_axis(S)
    x = np.zeros_like(t)
    for f, a in [(110, 0.5), (165, 0.3), (220.5, 0.2), (330, 0.12), (495, 0.06)]:
        x += a * np.sin(2 * np.pi * cyc(f, S) * t) * lfo(t, cyc(rng.uniform(0.05, 0.2), S), rng.uniform(0, 6))
    shimmer = band_noise(len(t), 2500, 8000) * 0.04 * lfo(t, cyc(0.1, S)) ** 2
    L = x * 0.5 + band_noise(len(t), 150, 900) * 0.35 + shimmer
    R = x * 0.5 + band_noise(len(t), 150, 900) * 0.35 + shimmer[::-1]
    save("dust_hum", norm(L, 0.5), norm(R, 0.5))


def wild_night():
    S = 20.0
    n = int(SR * S)
    t = t_axis(S)
    L = np.zeros(n)
    R = np.zeros(n)
    for _ in range(5):
        f = rng.uniform(3900, 5100)
        rate = cyc(rng.uniform(14, 22), S)
        trill = np.clip(np.sin(2 * np.pi * rate * t + rng.uniform(0, 6)), 0, 1) ** 2
        phrase = (np.sin(2 * np.pi * cyc(rng.uniform(0.2, 0.5), S) * t + rng.uniform(0, 6)) > 0.2).astype(float)
        phrase = np.convolve(np.concatenate([phrase[-400:], phrase, phrase[:400]]), np.ones(400) / 400, "same")[400:-400]
        s = np.sin(2 * np.pi * cyc(f, S) * t) * trill * phrase * 0.06
        pan = rng.uniform(0.15, 0.85)
        L += s * (1 - pan)
        R += s * pan
    w = band_noise(n, 80, 500) * (0.2 + 0.8 * lfo(t, cyc(0.1, S))) * 0.4
    save("wild_night", norm(L + w, 0.5), norm(R + band_noise(n, 80, 500) * 0.25, 0.5))


if __name__ == "__main__":
    void_drone()
    for i in (1, 2, 3):
        whisper(i)
    war_drum()
    town_day()
    port_water()
    thunder_far()
    dust_hum()
    wild_night()
