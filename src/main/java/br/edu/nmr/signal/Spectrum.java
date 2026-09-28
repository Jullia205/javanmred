package br.edu.nmr.signal;

public class Spectrum {
    private final double[] frequencies;
    private final double[] magnitudes;

    public Spectrum(double[] frequencies, double[] magnitudes) {
        this.frequencies = frequencies;
        this.magnitudes = magnitudes;
    }

    public double[] getFrequencies() { return frequencies; }
    public double[] getMagnitudes() { return magnitudes; }
}