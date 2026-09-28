package br.edu.nmr;

import br.edu.nmr.io.ExperimentReader;
import br.edu.nmr.model.Experiment;
import br.edu.nmr.model.TimeSignal;

import java.nio.file.Path;

/**
 * Demonstração de linha de comando: carrega um par de arquivos .json/.bin
 * e imprime um resumo — útil para a validação manual pedida na Etapa 7
 * ("relatório de validação e comparação das primeiras, intermediárias e
 * últimas amostras de cada traço").
 *
 * Uso:
 *   java -cp target/classes br.edu.nmr.Main caminho/para/arquivo_sem_extensao
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: Main <caminho-base-sem-extensao>");
            System.exit(1);
        }

        ExperimentReader reader = new ExperimentReader();
        Experiment experiment = reader.load(Path.of(args[0]));

        System.out.println("Experimento: " + experiment.sourceName());
        System.out.println("Número de scans: " + experiment.numScans());
        System.out.printf("Taxa de amostragem efetiva: %.3f Hz%n", experiment.parameters().effectiveSampleRate());

        for (int i = 0; i < experiment.numScans(); i++) {
            TimeSignal signal = experiment.scan(i);
            System.out.printf("  scan %d: %d canal(is), %d amostras/canal, duração=%.4f s%n",
                i, signal.numChannels(), signal.numSamples(), signal.durationSeconds());
            for (int ch = 0; ch < signal.numChannels(); ch++) {
                double[] data = signal.channel(ch);
                System.out.printf("    canal %d -> primeiras: [%.6g, %.6g, %.6g] ... últimas: [%.6g, %.6g, %.6g]%n",
                    ch,
                    data[0], data.length > 1 ? data[1] : Double.NaN, data.length > 2 ? data[2] : Double.NaN,
                    data[data.length - 3 >= 0 ? data.length - 3 : 0],
                    data[data.length - 2 >= 0 ? data.length - 2 : 0],
                    data[data.length - 1]);
            }
        }
    }

    private Main() {
    }
}
