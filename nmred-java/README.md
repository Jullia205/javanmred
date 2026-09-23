```markdown
# nmred-io — Módulo de Engenharia de Dados, I/O e Formatação

Módulo responsável pela leitura e validação dos dados brutos do experimento
de RMN (arquivos `.json` + `.bin`), conforme a Etapa 14 do roteiro do
projeto. Corresponde aos pacotes `io/` e `model/` da arquitetura definida
para o time.

Este módulo cobre exclusivamente **leitura, validação e modelagem de
dados**. Processamento de sinal (filtro, FFT, integração, ajuste de
curvas) fica a cargo dos pacotes `signal/`, `analysis/` e `service/`,
desenvolvidos pelo restante da equipe.

## Estrutura

```
src/main/java/br/edu/nmr/
model/
ParamMap.java              - wrapper tipado sobre um objeto JSON (idioma dict.get(key, default))
ExperimentParameters.java  - as 5 seções de parâmetros: p, pproc, pinc, ppre, pstat (nessa ordem)
TimeSignal.java            - um traço no domínio do tempo (canais x amostras)
Experiment.java            - parâmetros + lista de scans de um experimento
io/
json/MinimalJsonParser.java - parser de JSON sem dependências externas
ParameterReader.java        - lê o .json -> ExperimentParameters
BinarySignalReader.java     - lê o .bin (float64) -> double[]
ExperimentReader.java       - orquestra os dois acima -> Experiment
DataValidationException.java
Main.java                     - demo de linha de comando
src/test/java/br/edu/nmr/io/
ParameterReaderTest.java
BinarySignalReaderTest.java
ExperimentReaderTest.java
```

## Base da implementação

A leitura foi modelada a partir do código Python original do repositório
`pynmred` (Hilty Lab), especificamente `nmrbase/expbase.py` (método
`load()`) e `nmrbase/expdta.py`. A lógica de reshape dos dados binários,
os fallbacks de parâmetros (`nsamp = p['nsamp'] - p['nskip']` quando
`pstat['nsampx']` não existe) e a ordem das 5 seções do JSON (`p, pproc,
pinc, ppre, pstat`) seguem exatamente o comportamento do código Python,
para garantir equivalência entre as duas implementações.

## Formato dos dados

- **Arquivo `.json`**: array com 5 objetos, nessa ordem fixa:
  `[p (acquisition), pproc (processing), pinc (increment), ppre (pre-acquisition), pstat (status)]`.
- **Arquivo `.bin`**: amostras em `float64`, little-endian (`IEEE-754`,
  equivalente ao `dtype='<f8'` do NumPy), um array plano que é
  reorganizado em `[scan][canal][amostra]` a partir dos metadados do JSON.

## Por que um parser de JSON próprio em vez de Jackson

O parser (`io/json/MinimalJsonParser`) é uma implementação enxuta (sem
dependências externas) que cobre integralmente a estrutura dos arquivos de
parâmetro deste projeto (objetos, arrays, números, strings, booleanos,
null). Isso elimina uma dependência externa do módulo de I/O.

Caso o grupo prefira usar Jackson (citado como uma das opções possíveis na
Etapa 14 do roteiro), a troca é direta: (1) descomentar a dependência no
`pom.xml`, e (2) substituir o corpo de `ParameterReader.read()` por um
`ObjectMapper().readValue(file, new TypeReference<List<Map<String,Object>>>(){})`.
O restante do código (`model/`, `BinarySignalReader`, `ExperimentReader`)
não precisa de nenhuma alteração, pois depende apenas de
`ParamMap`/`ExperimentParameters`, não da forma como o JSON é parseado.

## Tratamento de erros

Todas as falhas de leitura/validação lançam `DataValidationException`, que
sempre inclui o caminho do arquivo e a causa específica do problema
(arquivo ausente, JSON malformado, campo obrigatório ausente, arquivo
truncado, valores não finitos), conforme exigido na Etapa 7 do roteiro.

## Validação com dados reais — CONCLUÍDA ✅

A validação foi feita com o pacote oficial de dados fornecido pelo
professor (`Equipe03_Dados_NMR_Alunos.zip`, conjuntos T1, T2 e Difusão).
Resultado: **300.200 amostras comparadas, erro zero, idêntico bit a bit ao
Python nos 3 conjuntos.** O relatório completo, com tabelas e metodologia,
está em [`RELATORIO_VALIDACAO.md`](./RELATORIO_VALIDACAO.md) — esse
documento corresponde ao "Resultado da etapa" pedido na Etapa 7 do
roteiro.

## Como rodar

```bash
mvn compile
mvn test          # roda os testes em src/test/

# com um par exp.json + exp.bin real:
java -cp target/classes br.edu.nmr.Main caminho/para/exp
```

## Próximos passos

1. Integrar este módulo com os pacotes `signal/` e `analysis/` do restante
   da equipe, que consomem `Experiment` e `TimeSignal` diretamente.
2. Repetir a validação (Seção "Validação com dados reais") quando
   dados experimentais adicionais forem disponibilizados.
```