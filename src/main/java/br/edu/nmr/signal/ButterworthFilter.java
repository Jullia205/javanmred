package br.edu.nmr.signal;

import br.edu.nmr.model.ExperimentParameters;
import br.edu.nmr.model.TimeSignal;
import uk.me.berndporr.iirj.Butterworth;

public class ButterworthFilter implements DigitalFilter {

    private final ExperimentParameters params;
    private final int ORDER = 4; // Quarta ordem, conforme o roteiro

    public ButterworthFilter(ExperimentParameters params) {
        this.params = params;
    }

    @Override
    public TimeSignal apply(TimeSignal inputSignal) {
        // 1. Extrair limites do filtro e calcular a taxa de amostragem
        double fMin = params.processing().getDouble("digfmin", 1500.0);
        double fMax = params.processing().getDouble("digfmax", 3500.0);

        // srate é o inverso de dx (1 / dx)
        double sRate = 1.0 / inputSignal.dx();

        // O IIRJ usa Frequência Central e Largura de Banda
        double centerFreq = (fMin + fMax) / 2.0;
        double width = fMax - fMin;

        int numChannels = inputSignal.numChannels();
        int numSamples = inputSignal.numSamples();

        // Matriz para guardar o resultado filtrado
        double[][] filteredData = new double[numChannels][numSamples];

        // 3. Processar cada canal de forma independente
        for (int ch = 0; ch < numChannels; ch++) {

            // Puxa o canal usando o método seguro da sua classe
            double[] originalChannel = inputSignal.channel(ch);

            // Instanciar o filtro passa-faixa
            Butterworth butterworth = new Butterworth();
            butterworth.bandPass(ORDER, sRate, centerFreq, width);

            // PASSO A: Filtragem para a frente (Forward)
            double[] forwardFiltered = new double[numSamples];
            for (int i = 0; i < numSamples; i++) {
                forwardFiltered[i] = butterworth.filter(originalChannel[i]);
            }

            // Instanciar um novo filtro para a passagem inversa (limpar a memória de estado do filtro)
            Butterworth butterworthReverse = new Butterworth();
            butterworthReverse.bandPass(ORDER, sRate, centerFreq, width);

            // PASSO B: Filtragem para trás (Backward) para obter fase zero
            for (int i = numSamples - 1; i >= 0; i--) {
                filteredData[ch][i] = butterworthReverse.filter(forwardFiltered[i]);
            }
        }

        // 4. Retornar um novo TimeSignal usando os mesmos dx() e x0() originais
        return new TimeSignal(filteredData, inputSignal.dx(), inputSignal.x0());
    }
}