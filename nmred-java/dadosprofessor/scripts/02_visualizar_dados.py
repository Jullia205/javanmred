from pathlib import Path
import json
import numpy as np
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "saidas"
OUT.mkdir(exist_ok=True)


def carregar(nome_base: Path):
    with open(nome_base.with_suffix(".json"), "r", encoding="utf-8") as f:
        p, pproc, pinc, ppre, pstat = json.load(f)
    dados = np.fromfile(nome_base.with_suffix(".bin"), dtype="<f8")
    nch = int(pstat["ninchann"])
    nsamp = int(pstat.get("nsampx", p["nsamp"] - p.get("nskip", 0)))
    nscan = dados.size // (nch * nsamp)
    return p, pproc, pinc, ppre, pstat, dados.reshape(nscan, nch, nsamp)


bases = {
    "T1": ROOT / "dados" / "T1" / "T1_referencia_sintetica",
    "T2": ROOT / "dados" / "T2" / "T2_referencia_sintetica",
    "Difusao": ROOT / "dados" / "Difusao" / "Difusao_referencia_sintetica",
}

for nome, base in bases.items():
    p, pproc, pinc, ppre, pstat, dados = carregar(base)
    sr = float(pstat.get("sratex", p["srate"]))
    y = dados[0, 0]
    t = np.arange(y.size) / sr

    fig = plt.figure(figsize=(9, 4.8))
    plt.plot(t, y, linewidth=0.8)
    plt.xlabel("Tempo (s)")
    plt.ylabel("Amplitude (u.a.)")
    plt.title(f"{nome} - primeiro sinal carregado")
    plt.grid(alpha=0.25)
    fig.tight_layout()
    arq = OUT / f"{nome}_primeiro_sinal.png"
    fig.savefig(arq, dpi=160)
    plt.close(fig)
    print(f"Gerado: {arq}")

print("Visualizacoes concluidas.")
