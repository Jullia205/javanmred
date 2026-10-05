package br.edu.nmr.signal;

import br.edu.nmr.model.ExperimentParameters;
import br.edu.nmr.model.TimeSignal;

public class Windowing {

    /**
     * Extrai a janela temporal do sinal com base nos parâmetros 'ftmin' e 'ftmax'.
     * Equivalente ao recorte de array antes da FFT no Python.
     *
     * @param inputSignal O sinal no tempo (pode ser o bruto ou o já filtrado)
     * @param params Parâmetros do experimento contendo a seção 'pproc'
     * @return Um novo TimeSignal contendo apenas a região recortada
     */
    public static TimeSignal extractEcho(TimeSignal inputSignal, ExperimentParameters params) {

        // 1. Lemos os parâmetros de tempo mínimo e máximo (em segundos)
        double tMin = params.processing().getDouble("ftmin", 0.0);
        double tMax = params.processing().getDouble("ftmax", 0.0);

        // No Python original, ftmax = 0 significa "usar até o final do dado"
        if (tMax <= 0.0) {
            tMax = inputSignal.x0() + inputSignal.durationSeconds();
        }

        // 2. Convertendo os tempos físicos para índices do array usando o método da sua classe
        int startIndex = inputSignal.timeToIndex(tMin);
        int endIndex = inputSignal.timeToIndex(tMax);

        // Validação de segurança para evitar arrays de tamanho negativo ou zero
        if (startIndex >= endIndex) {
            throw new IllegalArgumentException(
                    String.format("Janela de tempo inválida. ftmin (%.4f s) -> índice %d, ftmax (%.4f s) -> índice %d.",
                            tMin, startIndex, tMax, endIndex)
            );
        }

        int newNumSamples = endIndex - startIndex;
        int numChannels = inputSignal.numChannels();

        // Matriz que guardará apenas o trecho recortado
        double[][] windowedData = new double[numChannels][newNumSamples];

        // 3. Recortando canal por canal
        for (int ch = 0; ch < numChannels; ch++) {
            // Puxa o array inteiro do canal (cópia defensiva da sua classe)
            double[] originalChannel = inputSignal.channel(ch);

            // Copia apenas do 'startIndex' até o limite do 'newNumSamples'
            System.arraycopy(originalChannel, startIndex, windowedData[ch], 0, newNumSamples);
        }

        // 4. Calculando o novo tempo inicial (x0) da janela recortada
        double newX0 = inputSignal.x0() + (startIndex * inputSignal.dx());

        // Retorna o novo objeto encapsulado
        return new TimeSignal(windowedData, inputSignal.dx(), newX0);
    }
}