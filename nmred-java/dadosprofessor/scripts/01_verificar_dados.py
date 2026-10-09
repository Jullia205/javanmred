from pathlib import Path
import json
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
DATASETS = {
    "T1": ROOT / "dados" / "T1" / "T1_referencia_sintetica",
    "T2": ROOT / "dados" / "T2" / "T2_referencia_sintetica",
    "Difusao": ROOT / "dados" / "Difusao" / "Difusao_referencia_sintetica",
}


def carregar(nome_base: Path):
    json_file = nome_base.with_suffix(".json")
    bin_file = nome_base.with_suffix(".bin")

    if not json_file.exists():
        raise FileNotFoundError(f"JSON nao encontrado: {json_file}")
    if not bin_file.exists():
        raise FileNotFoundError(f"BIN nao encontrado: {bin_file}")

    with open(json_file, "r", encoding="utf-8") as f:
        p, pproc, pinc, ppre, pstat = json.load(f)

    dados = np.fromfile(bin_file, dtype="<f8")
    nch = int(pstat["ninchann"])
    nsamp = int(pstat.get("nsampx", p["nsamp"] - p.get("nskip", 0)))

    bloco = nch * nsamp
    if dados.size % bloco != 0:
        raise ValueError(
            f"Tamanho incompativel: {dados.size} valores nao formam blocos de {bloco}."
        )

    nscan = dados.size // bloco
    dados = dados.reshape(nscan, nch, nsamp)

    return p, pproc, pinc, ppre, pstat, dados


for nome, base in DATASETS.items():
    p, pproc, pinc, ppre, pstat, dados = carregar(base)
    print("=" * 72)
    print(nome)
    print(f"Arquivo-base : {base}")
    print(f"Shape        : {dados.shape}  (tracos, canais, amostras)")
    print(f"dtype        : {dados.dtype}")
    print(f"srate        : {pstat.get('sratex', p.get('srate'))} Hz")
    print(f"min / max    : {dados.min():.6f} / {dados.max():.6f}")
    print(f"5 primeiras  : {dados.reshape(-1)[:5]}")

print("=" * 72)
print("VERIFICACAO CONCLUIDA: todos os pares JSON/BIN foram lidos com sucesso.")
