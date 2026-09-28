package br.edu.nmr.io;

import br.edu.nmr.model.Experiment;
import br.edu.nmr.model.ExperimentParameters;
import br.edu.nmr.model.ParamMap;
import br.edu.nmr.model.TimeSignal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê um par de arquivos (base.json + base.bin) e monta um {@link Experiment}
 * completo. É o equivalente Java de {@code expbase.load(filename)}
 * (nmrbase/expbase.py, linhas 165-190 do Python original).
 *
 * <h2>Lógica original (Python), para referência</h2>
 * <pre>
 *   dta = np.fromfile(filename0, dtype='float64')
 *   nchann = self.pstat['ninchann']
 *   srate = self.getsrate()                      # pstat['sratex'] ou p['srate']
 *   nsamp = self.pstat.get('nsampx', None)
 *   if nsamp is None:
 *       nsamp = self.p['nsamp'] - self.p.get('nskip', 0)
 *   nscan = np.size(dta) // nchann // nsamp
 *   lgt = nchann * nsamp
 *   for i in range(nscan):
 *       dt = dta[i*lgt:(i+1)*lgt].reshape([nchann, nsamp])
 *       self.tdt.append(timedata(dt, 1/srate))
 * </pre>
 *
 * Note que a divisão {@code nscan = size // nchann // nsamp} em Python é
 * inteira (floor division) — se sobrarem bytes que não completam mais um
 * scan inteiro, eles são simplesmente descartados, sem erro. Reproduzimos
 * esse mesmo comportamento aqui, mas REGISTRAMOS quando isso acontece,
 * porque normalmente é sinal de arquivo truncado ou parâmetro errado
 * (é exatamente um dos casos de teste pedidos na Etapa 13: "arquivo truncado").
 */
public final class ExperimentReader {

    private final boolean rejectNonFiniteSamples;

    public ExperimentReader() {
        this(true);
    }

    /**
     * @param rejectNonFiniteSamples se true (padrão), lança exceção quando o traço
     *                               contém NaN/Infinity. Se sua equipe decidir tratar
     *                               isso de outra forma rio abaixo (ex.: no pipeline de
     *                               processamento), passe false aqui e trate explicitamente.
     */
    public ExperimentReader(boolean rejectNonFiniteSamples) {
        this.rejectNonFiniteSamples = rejectNonFiniteSamples;
    }

    /**
     * @param basePath caminho SEM extensão (ou com .json/.bin — a extensão é ignorada),
     *                 igual ao comportamento de {@code os.path.splitext} no Python.
     */
    public Experiment load(Path basePath) {
        Path base = stripExtension(basePath);
        Path jsonPath = base.resolveSibling(base.getFileName() + ".json");
        Path binPath = base.resolveSibling(base.getFileName() + ".bin");

        ExperimentParameters params = ParameterReader.read(jsonPath);
        double[] flatData = BinarySignalReader.readFloat64(binPath);

        List<TimeSignal> scans = reshapeIntoScans(binPath, params, flatData);

        return new Experiment(params, scans, base.getFileName().toString());
    }

    private List<TimeSignal> reshapeIntoScans(Path binPath, ExperimentParameters params, double[] flatData) {
        ParamMap p = params.acquisition();
        ParamMap pstat = params.status();

        if (!pstat.has("ninchann")) {
            throw new DataValidationException(binPath.toString(),
                "parâmetro obrigatório 'ninchann' (número de canais) ausente em pstat");
        }
        int numChannels = pstat.requireInt("ninchann");
        if (numChannels <= 0) {
            throw new DataValidationException(binPath.toString(),
                "'ninchann' deveria ser positivo, veio: " + numChannels);
        }

        double sampleRate = params.effectiveSampleRate();
        if (sampleRate <= 0) {
            throw new DataValidationException(binPath.toString(),
                "taxa de amostragem efetiva deveria ser positiva, veio: " + sampleRate);
        }

        int numSamplesPerChannel;
        if (pstat.has("nsampx")) {
            numSamplesPerChannel = pstat.requireInt("nsampx");
        } else {
            int nsampTotal = p.requireInt("nsamp");
            int skip = p.getInt("nskip", 0);
            numSamplesPerChannel = nsampTotal - skip;
        }
        if (numSamplesPerChannel <= 0) {
            throw new DataValidationException(binPath.toString(),
                "número de amostras por canal calculado é <= 0 (" + numSamplesPerChannel
                    + ") — confira 'nsampx'/'nsamp'/'nskip' no JSON");
        }

        long samplesPerScan = (long) numChannels * numSamplesPerChannel;
        long totalSamplesInFile = flatData.length;
        long numScans = totalSamplesInFile / samplesPerScan;
        long leftover = totalSamplesInFile % samplesPerScan;

        if (numScans == 0) {
            throw new DataValidationException(binPath.toString(),
                "o arquivo tem " + totalSamplesInFile + " amostras, mas cada scan precisa de "
                    + samplesPerScan + " (canais=" + numChannels + " x amostras/canal="
                    + numSamplesPerChannel + "); não dá para montar nem 1 scan completo "
                    + "— arquivo provavelmente truncado ou parâmetros errados");
        }
        if (leftover != 0) {
            // Mesmo comportamento do Python (divisão inteira descarta o resto),
            // mas isso quase sempre indica arquivo truncado: reportar, não silenciar.
            System.err.printf(
                "[ExperimentReader] aviso: %s tem %d amostra(s) sobrando após "
                    + "montar %d scan(s) completo(s) — possível arquivo truncado.%n",
                binPath, leftover, numScans);
        }

        double dx = 1.0 / sampleRate;
        List<TimeSignal> scans = new ArrayList<>();
        for (int scanIndex = 0; scanIndex < numScans; scanIndex++) {
            double[][] reshaped = new double[numChannels][numSamplesPerChannel];
            long scanOffset = scanIndex * samplesPerScan;
            // numpy .reshape([nchann, nsamp]) em um array C-contíguo é row-major:
            // canal 0 ocupa as primeiras `nsamp` posições, depois canal 1, etc.
            for (int ch = 0; ch < numChannels; ch++) {
                long channelOffset = scanOffset + (long) ch * numSamplesPerChannel;
                for (int s = 0; s < numSamplesPerChannel; s++) {
                    reshaped[ch][s] = flatData[(int) (channelOffset + s)];
                }
            }
            TimeSignal signal = new TimeSignal(reshaped, dx, 0.0);
            if (rejectNonFiniteSamples) {
                long nonFinite = signal.countNonFiniteValues();
                if (nonFinite > 0) {
                    throw new DataValidationException(binPath.toString(),
                        "scan " + scanIndex + " contém " + nonFinite
                            + " valor(es) não finito(s) (NaN/Infinity)");
                }
            }
            scans.add(signal);
        }
        return scans;
    }

    private static Path stripExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot <= 0) {
            return path;
        }
        String withoutExt = name.substring(0, dot);
        Path parent = path.getParent();
        return parent == null ? Path.of(withoutExt) : parent.resolve(withoutExt);
    }
}
