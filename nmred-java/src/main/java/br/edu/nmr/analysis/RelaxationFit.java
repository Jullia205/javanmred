package br.edu.nmr.analysis;

/**
 * Ajuste de relaxacao y(t) = A exp(-R t) + C, T = 1/R (Etapa 11 do roteiro).
 * T1: crescimento (A<0). T2: decaimento dos ecos (A>0).
 * Valores iniciais seguem o notebook: T1 p0=[-y_fim, 0.4, y_fim].
 */
public final class RelaxationFit {

    public static final ModelFunction MODEL = new ModelFunction() {
        public int numParameters() { return 3; }
        public double value(double t, double[] p) { return p[0] * Math.exp(-p[1] * t) + p[2]; }
        public double[] gradient(double t, double[] p) {
            double e = Math.exp(-p[1] * t);
            return new double[]{e, -p[0] * t * e, 1.0};
        }
    };

    /** A, R (s^-1), C, T = 1/R (s) e incertezas (1 sigma, propagacao de erro para T). */
    public record Result(double a, double r, double c, double t,
                         double sigmaA, double sigmaR, double sigmaC, double sigmaT,
                         FitResult fit) {}

    public Result fitT1(double[] times, double[] integrals) {
        double last = integrals[integrals.length - 1];
        return fit(times, integrals, new double[]{-last, 0.4, last});
    }

    /** T2: decaimento; chute inicial A=y0, R=1/(tempo medio), C=0 (ajuste via p0 customizavel). */
    public Result fitT2(double[] times, double[] integrals) {
        double span = times[times.length - 1] - times[0];
        return fit(times, integrals, new double[]{integrals[0], span > 0 ? 3.0 / span : 1.0, 0.0});
    }

    public Result fit(double[] times, double[] y, double[] p0) {
        FitResult f = new LevenbergMarquardt().fit(MODEL, times, y, p0);
        double[] p = f.params(), s = f.stdErrors();
        return new Result(p[0], p[1], p[2], 1.0 / p[1], s[0], s[1], s[2], s[1] / (p[1] * p[1]), f);
    }
}
