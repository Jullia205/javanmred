package br.edu.nmr.signal;

import br.edu.nmr.model.TimeSignal;
import org.jtransforms.fft.DoubleFFT_1D;

public class FourierTransform {

    /**
     * Aplica a FFT a um sinal no domínio do tempo e converte-o para o domínio da frequência.
     * Corresponde à Etapa 9 do roteiro.
     */
    public Spectrum transform(TimeSignal timeSignal) {
        // Para este guião de RMN de campo baixo, os dados relevantes estão habitualmente no canal 0.
        double[] originalData = timeSignal.channel(0);
        int n = originalData.length;

        /*
         * A JTransforms tem métodos otimizados. Para usarmos complexForward de forma legível
         * e calcularmos a magnitude facilmente, criamos um array de tamanho 2N.
         * O layout de memória esperado é: [Re0, Im0, Re1, Im1, ..., ReN, ImN]
         */
        double[] fftData = new double[2 * n];
        for (int i = 0; i < n; i++) {
            fftData[2 * i] = originalData[i]; // Parte real recebe o valor do sinal
            fftData[2 * i + 1] = 0.0;         // Parte imaginária inicializada a zero
        }

        // Instanciar a FFT para o tamanho N do sinal
        DoubleFFT_1D fft = new DoubleFFT_1D(n);

        // Executa a transformada (os valores em fftData são substituídos pelo resultado)
        fft.complexForward(fftData);

        // Em sinais reais, só a primeira metade do espetro (frequências positivas) é útil
        int halfN = n / 2;
        double[] magnitudes = new double[halfN];
        double[] frequencies = new double[halfN];

        // A taxa de amostragem (srate) é 1 a dividir pelo espaçamento de tempo (dx)
        double sRate = 1.0 / timeSignal.dx();

        // A resolução de frequência (Delta f) de cada "bin" do espetro
        double deltaF = sRate / n;

        for (int k = 0; k < halfN; k++) {
            double re = fftData[2 * k];
            double im = fftData[2 * k + 1];

            // Cálculo da Magnitude: M = sqrt(Re^2 + Im^2)
            magnitudes[k] = Math.sqrt(re * re + im * im);

            // Geração do eixo X (Frequência em Hz)
            frequencies[k] = k * deltaF;
        }

        return new Spectrum(frequencies, magnitudes);
    }
}