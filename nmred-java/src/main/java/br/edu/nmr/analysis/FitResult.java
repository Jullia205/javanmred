package br.edu.nmr.analysis;

/** Resultado de um ajuste nao linear (equivalente a popt/pcov do scipy.curve_fit). */
public record FitResult(double[] params, double[] stdErrors, double[][] covariance,
                        double[] residuals, double rss, double rSquared,
                        int iterations, boolean converged) {
    public double[] fitted(double[] x, ModelFunction m) {
        double[] y = new double[x.length];
        for (int i = 0; i < x.length; i++) y[i] = m.value(x[i], params);
        return y;
    }
}
