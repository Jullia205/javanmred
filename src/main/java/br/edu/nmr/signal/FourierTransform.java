package br.edu.nmr.signal;

import br.edu.nmr.model.TimeSignal;
import org.jtransforms.fft.DoubleFFT_1D;

public class FourierTransform {

    public Spectrum transform(TimeSignal timeSignal) {
        // Para este guião de RMN de campo baixo, os dados relevantes estão habitualmente no canal 0.
        double[] originalData = timeSignal.channel(0);
        int n = originalData.length;

        double[] fftData = new double[2 * n];
        for (int i = 0; i < n; i++) {
            fftData[2 * i] = originalData[i]; // Parte real recebe o valor do sinal
            fftData[2 * i + 1] = 0.0;         // Parte imaginária inicializada a zero
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(n);

        fft.complexForward(fftData);

        int halfN = n / 2;
        double[] magnitudes = new double[halfN];
        double[] frequencies = new double[halfN];

        double sRate = 1.0 / timeSignal.dx();

        double deltaF = sRate / n;

        for (int k = 0; k < halfN; k++) {
            double re = fftData[2 * k];
            double im = fftData[2 * k + 1];

            magnitudes[k] = Math.sqrt(re * re + im * im);

            frequencies[k] = k * deltaF;
        }

        return new Spectrum(frequencies, magnitudes);
    }
}