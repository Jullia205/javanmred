package br.edu.nmr.model;

/**
 * Representa os dados observáveis após a integração dos picos (Etapa 6).
 * Associa o eixo X (parâmetro experimental variado) ao eixo Y (áreas integradas).
 */
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