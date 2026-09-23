# Relatório de Validação — Leitor Java vs. Python (Etapa 7 / Etapa 15)

**Módulo:** Engenharia de Dados, I/O e Formatação (`nmred-io`)
**Dados usados:** pacote oficial fornecido pelo professor (`Equipe03_Dados_NMR_Alunos.zip`)
**Data da validação:** conferir junto ao commit/entrega

## 1. Integridade dos arquivos de entrada

Os checksums SHA-256 dos arquivos recebidos foram conferidos contra
`SHA256_DADOS.txt` incluído no pacote do professor — todos batem:

| Arquivo | SHA-256 confere? |
|---|---|
| `dados/T1/T1_referencia_sintetica.bin` | ✅ |
| `dados/T1/T1_referencia_sintetica.json` | ✅ |
| `dados/T2/T2_referencia_sintetica.bin` | ✅ |
| `dados/T2/T2_referencia_sintetica.json` | ✅ |
| `dados/Difusao/Difusao_referencia_sintetica.bin` | ✅ |
| `dados/Difusao/Difusao_referencia_sintetica.json` | ✅ |

## 2. Ordem de bytes (endianness)

O `LEIA-ME.txt` do pacote confirma o formato: `IEEE-754 float64
little-endian (dtype='<f8')`. Essa é a mesma suposição que o leitor Java já
usava por padrão (`BinarySignalReader`, `ByteOrder.LITTLE_ENDIAN`) — **não
foi necessário nenhum ajuste**. A comparação da Seção 4 confirma isso na
prática (erro zero).

## 3. Metodologia da comparação

Para cada um dos 3 conjuntos de dados (T1, T2, Difusão):

1. O par `.json`/`.bin` foi carregado em Python com a mesma lógica de
   `expbase.load()` (parâmetros → `nchann`, `srate`, `nsamp`; reshape do
   array plano em `[nscan, nchann, nsamp]`).
2. O mesmo par de arquivos foi carregado com `ExperimentReader.load()`
   (Java, módulo `nmred-io`).
3. Cada amostra individual (todas, não só uma amostragem) foi comparada
   numericamente: `erro_abs = |valor_python - valor_java|`.

## 4. Resultado — comparação completa, amostra a amostra

| Conjunto | Programa de pulso | Canais | Amostras/canal | Scans | Total de amostras comparadas | Erro absoluto máximo | Idêntico bit a bit? |
|---|---|---|---|---|---|---|---|
| T1 | `t1` | 1 | 10.000 | 11 | 110.000 | `0.000e+00` | ✅ Sim |
| T2 | `cpmg` | 1 | 70.200 | 1 | 70.200 | `0.000e+00` | ✅ Sim |
| Difusão | `diffusion` | 1 | 10.000 | 12 | 120.000 | `0.000e+00` | ✅ Sim |

**Total geral: 300.200 amostras comparadas, 0 divergências.**

### Detalhe — primeiras/últimas amostras (para inspeção visual)

**T1, scan 0** (primeiro dos 11 scans, período de pré-polarização mais curto)

| | amostra 1 | amostra 2 | amostra 3 | ... | antepenúltima | penúltima | última |
|---|---|---|---|---|---|---|---|
| Python | -0.03691443231 | 0.02708859468 | -0.005075248067 | | 0.002221334299 | 0.04750464246 | -0.009199930143 |
| Java   | -0.03691443231 | 0.02708859468 | -0.005075248067 | | 0.002221334299 | 0.04750464246 | -0.009199930143 |

**T1, scan 10** (último dos 11 scans, período de pré-polarização mais longo)

| | amostra 1 | amostra 2 | amostra 3 | ... | antepenúltima | penúltima | última |
|---|---|---|---|---|---|---|---|
| Python | 0.01928776668 | -0.01750839022 | 0.02143471702 | | -0.01017565511 | -0.0330516743 | -0.01469073789 |
| Java   | 0.01928776668 | -0.01750839022 | 0.02143471702 | | -0.01017565511 | -0.0330516743 | -0.01469073789 |

**T2, scan 0** (traço único, será dividido nos ecos pela etapa de `split`/filtro — fora do escopo deste módulo)

| | amostra 1 | amostra 2 | amostra 3 | ... | antepenúltima | penúltima | última |
|---|---|---|---|---|---|---|---|
| Python | -0.0009654244625 | -0.02804037772 | -0.0354052446 | | 0.005270795465 | 0.01048385273 | -0.03091981747 |
| Java   | -0.0009654244625 | -0.02804037772 | -0.0354052446 | | 0.005270795465 | 0.01048385273 | -0.03091981747 |

**Difusão, scan 0 e scan 11** (primeiro e último dos 12 incrementos de gradiente)

| | amostra 1 | amostra 2 | amostra 3 | ... | antepenúltima | penúltima | última |
|---|---|---|---|---|---|---|---|
| Python (scan 0)  | -0.02354110895 | -0.0004487622495 | 0.005049581903 | | 0.001885799955 | -0.0009729836306 | -0.01290535354 |
| Java (scan 0)    | -0.02354110895 | -0.0004487622495 | 0.005049581903 | | 0.001885799955 | -0.0009729836306 | -0.01290535354 |
| Python (scan 11) | 0.002412488539 | -0.01811326809 | -0.004986657756 | | 0.03976101097 | 0.0004173790891 | -0.006835880105 |
| Java (scan 11)   | 0.002412488539 | -0.01811326809 | -0.004986657756 | | 0.03976101097 | 0.0004173790891 | -0.006835880105 |

## 5. Metadados e dimensões — conferência

| Conjunto | `ninchann` | `nsampx`/`nsamp` efetivo | `sratex`/`srate` efetivo | dx = 1/srate |
|---|---|---|---|---|
| T1 | 1 | 10.000 | 21.600 Hz | 4,6296e-05 s |
| T2 | 1 | 70.200 | 21.600 Hz | 4,6296e-05 s |
| Difusão | 1 | 10.000 | 21.600 Hz | 4,6296e-05 s |

Número de scans reconstruídos bateu com o parâmetro `n` de `pinc` em todos
os casos (T1: 11, T2: 1, Difusão: 12), confirmando que a lógica de reshape
(`nscan = tamanho_total // nchann // nsamp`) está correta.

## 6. Casos de erro testados (dados sintéticos, não os do professor)

Além da validação com dados reais acima, o leitor foi testado contra os
casos de falha exigidos na Etapa 13 do roteiro:

| Caso | Comportamento observado |
|---|---|
| Arquivo `.bin` ausente | `DataValidationException` com o caminho do arquivo na mensagem |
| Arquivo `.json` ausente | `DataValidationException` com o caminho do arquivo na mensagem |
| JSON malformado | `DataValidationException` com a posição do erro de sintaxe |
| Arquivo truncado (menos amostras que 1 scan completo) | `DataValidationException` explicando quantas amostras faltam |
| Valores não finitos (NaN/Infinity) no meio do traço | Rejeitado por padrão; aceitável se configurado explicitamente |
| Metadado obrigatório ausente (`ninchann`) | `DataValidationException` nomeando o campo ausente |

## 7. Conclusão

O leitor Java (`nmred-io`, pacotes `model/` e `io/`) reproduz **exatamente**
(erro zero, bit a bit) a leitura feita pelo código Python original
(`expbase.load()`) nos três conjuntos de dados de referência fornecidos
pelo professor. A suposição de endianness (little-endian) estava correta e
foi confirmada tanto pela documentação do pacote de dados quanto pela
comparação numérica.

**Este módulo está validado e pronto para ser consumido pelo restante da
equipe** (pacotes `signal/`, `analysis/`, `service/`).
