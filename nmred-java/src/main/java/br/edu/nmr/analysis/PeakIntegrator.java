package br.edu.nmr.analysis;

/**
 * Integral de pico em [fmin, fmax). Regra original do Python = soma de bins
 * (equivalencia); integralWithUnit multiplica por delta f (Etapa 10).
 * Recebe magnitude e eixo ja prontos (espectro vem do pacote signal/).
 */
public final class PeakIntegrator {
    public static double sumBins(double[] freq, double[] mag, double fmin, double fmax) {
        if (freq.length != mag.length) throw new IllegalArgumentException("freq e mag com tamanhos diferentes");
        double s = 0; int count = 0;
        for (int i = 0; i < freq.length; i++)
            if (freq[i] >= fmin && freq[i] < fmax) { s += mag[i]; count++; }
        if (count == 0) throw new IllegalArgumentException("Faixa [" + fmin + ", " + fmax + ") sem pontos no espectro");
        return s;
    }

    public static double integralWithUnit(double[] freq, double[] mag, double fmin, double fmax) {
        if (freq.length < 2) throw new IllegalArgumentException("Eixo com menos de 2 pontos");
        return sumBins(freq, mag, fmin, fmax) * (freq[1] - freq[0]);
    }
    private PeakIntegrator() {}
}
