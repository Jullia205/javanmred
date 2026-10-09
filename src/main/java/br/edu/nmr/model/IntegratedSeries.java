package br.edu.nmr.model;

public class IntegratedSeries {
    private final double[] xValues;
    private final double[] yValues;

    public IntegratedSeries(double[] xValues, double[] yValues) {
        this.xValues = xValues;
        this.yValues = yValues;
    }

    public double[] getXValues() { return xValues; }
    public double[] getYValues() { return yValues; }
}