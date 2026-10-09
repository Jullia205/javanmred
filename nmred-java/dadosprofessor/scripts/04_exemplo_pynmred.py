"""
Exemplo para usar os dados com o repositorio oficial Hilty-lab/pynmred.

Antes de executar:
1) clone/baixe o repositorio pynmred;
2) ajuste PYNMRED_DIR para a pasta do repositorio;
3) mantenha este pacote de dados intacto.

O codigo oficial espera o NOME-BASE, sem .json ou .bin.
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
PYNMRED_DIR = Path(r"C:\CAMINHO\PARA\pynmred")  # ALTERE AQUI

sys.path.insert(0, str(PYNMRED_DIR))

# Dependendo da versao/estrutura do repositorio, ajuste o import abaixo.
from nmrbase import expbase

filename = ROOT / "dados" / "T1" / "T1_referencia_sintetica"

a = expbase.expbase()
a.load(str(filename))

print("Arquivo carregado pelo pynmred.")
print("Tipo do objeto:", type(a))
print("Parametros:", a.p)
print("Parametros de processamento:", a.pproc)
