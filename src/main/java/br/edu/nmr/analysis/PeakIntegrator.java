package br.edu.nmr.analysis;

import br.edu.nmr.model.ExperimentParameters;
import br.edu.nmr.signal.Spectrum;

public class PeakIntegrator {

    /**
     * Realiza a integração do pico no domínio da frequência (Etapa 10).
     *
     * @param spectrum Espectro gerado pela Transformada de Fourier (FFT)
     * @param params Parâmetros do experimento (para extrair intmin e intmax)
     * @return O valor da integração (soma das magnitudes)
     */
    public double integrate(Spectrum spectrum, ExperimentParameters params) {

        // 1. Obter os limites de integração escolhidos (em Hz) do JSON
        // Utilizamos valores de fallback padrão caso não existam
        double fMin = params.processing().getDouble("intmin", 2400.0);
        double fMax = params.processing().getDouble("intmax", 2850.0);

        double[] frequencies = spectrum.getFrequencies();
        double[] magnitudes = spectrum.getMagnitudes();

        double sum = 0.0;

        // Calcular a resolução em frequência (Delta f) para a área real
        double deltaF = 0.0;
        if (frequencies.length > 1) {
            deltaF = frequencies[1] - frequencies[0];
        }

        // 2. Iterar pelo espetro e somar as amplitudes que caem dentro da janela
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] >= fMin && frequencies[i] <= fMax) {
                sum += magnitudes[i];
            }
        }

        // 3. Cumprimento da Etapa 10 do Roteiro:
        // A integral com unidade física seria a soma multiplicada pela base (Delta f)
        double physicalIntegral = sum * deltaF;

        // Nota: O método devolve a "sum" simples para garantir que a curva
        // gerada nesta plataforma Java bata valor a valor com o algoritmo original em Python.
        // O valor physicalIntegral pode ser exposto caso a equipa queira adicionar isso à interface gráfica.

        return sum;
    }
}