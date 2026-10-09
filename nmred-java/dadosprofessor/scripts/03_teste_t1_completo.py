from pathlib import Path
import json
import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import butter, filtfilt
from scipy.optimize import curve_fit

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "dados" / "T1" / "T1_referencia_sintetica"
OUT = ROOT / "saidas"
OUT.mkdir(exist_ok=True)

with open(BASE.with_suffix(".json"), "r", encoding="utf-8") as f:
    p, pproc, pinc, ppre, pstat = json.load(f)

raw = np.fromfile(BASE.with_suffix(".bin"), dtype="<f8")
sr = float(pstat["sratex"])
nsamp = int(pstat["nsampx"])
nscan = raw.size // nsamp
data = raw.reshape(nscan, 1, nsamp)[:, 0, :]

# Filtro Butterworth de 4a ordem, zero-phase, semelhante ao fluxo do roteiro.
b, a = butter(4, [2000.0, 3500.0], btype="bandpass", fs=sr)
filtered = filtfilt(b, a, data, axis=-1)

# Janela temporal do eco e integracao do espectro.
tmin, tmax = 0.26, 0.34
fmin, fmax = 2550.0, 2850.0
integrals = []

for trace in filtered:
    i1 = int(round(tmin * sr))
    i2 = int(round(tmax * sr))
    x = trace[i1:i2]
    n = len(x)
    spec = np.abs(np.fft.fft(x)) * (2.0 / n)
    spec = spec[:round(n / 2)]
    freq = np.arange(len(spec)) * (sr / n)
    mask = (freq >= fmin) & (freq < fmax)
    integrals.append(spec[mask].sum())

integrals = np.asarray(integrals)
times = np.arange(nscan) * float(pinc["inc"][0]) + float(ppre.get("poltime", 0.0))


def model(t, A, R, C):
    return A * np.exp(-R * t) + C

p0 = [-integrals[-1], 0.4, integrals[-1]]
popt, pcov = curve_fit(model, times, integrals, p0=p0, maxfev=10000)
A, R1, C = popt
T1 = 1.0 / R1

print(f"R1 = {R1:.6f} s^-1")
print(f"T1 = {T1:.6f} s")

fig = plt.figure(figsize=(8, 5))
plt.scatter(times, integrals, label="Integracao")
tfit = np.linspace(times.min(), times.max(), 300)
plt.plot(tfit, model(tfit, *popt), label="Ajuste")
plt.xlabel("Tempo de polarizacao (s)")
plt.ylabel("Integral do pico (u.a.)")
plt.title("T1 - teste completo do conjunto de referencia")
plt.grid(alpha=0.25)
plt.legend()
fig.tight_layout()
fig.savefig(OUT / "T1_ajuste.png", dpi=160)
plt.close(fig)
