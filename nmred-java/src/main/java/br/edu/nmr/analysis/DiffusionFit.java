package br.edu.nmr.analysis;

/**
 * Stejskal-Tanner: I(G) = I0 exp[-D g^2 G^2 d^2 (Delta - d/3)] (Etapa 12).
 * Ajuste interno em y = I0 exp(-b G^2), com b = D g^2 d^2 (Delta - d/3) (parametro auxiliar);
 * D = b / (g^2 d^2 (Delta - d/3)). Unidades SI: G [T/m], gamma [rad s^-1 T^-1],
 * delta e Delta [s], D [m^2/s].
 */
public final class DiffusionFit {

    /** gamma do proton, rad s^-1 T^-1. */
    public static final double GAMMA_1H = 2.675221874e8;

    public static final ModelFunction MODEL = new ModelFunction() {
        public int numParameters() { return 2; }
        public double value(double g, double[] p) { return p[0] * Math.exp(-p[1] * g * g); }
        public double[] gradient(double g, double[] p) {
            double e = Math.exp(-p[1] * g * g);
            return new double[]{e, -p[0] * g * g * e};
        }
    };

    public record Result(double i0, double b, double d, double sigmaI0, double sigmaB, double sigmaD, FitResult fit) {}

    private final double gamma, delta, bigDelta;

    public DiffusionFit(double gamma, double delta, double bigDelta) {
        if (gamma <= 0 || delta <= 0 || bigDelta <= delta / 3)
            throw new IllegalArgumentException("Exige gamma>0, delta>0 e Delta>delta/3");
        this.gamma = gamma; this.delta = delta; this.bigDelta = bigDelta;
    }

    public double factor() { return gamma * gamma * delta * delta * (bigDelta - delta / 3.0); }

    public Result fit(double[] gradients, double[] intensities) {
        double max = 0;
        for (double v : intensities) max = Math.max(max, v);
        double gmax = 0;
        for (double g : gradients) gmax = Math.max(gmax, Math.abs(g));
        // b0 tal que o decaimento total seja ~ e^-2 (escala robusta)
        double b0 = gmax > 0 ? 2.0 / (gmax * gmax) : 1.0;
        FitResult f = new LevenbergMarquardt().fit(MODEL, gradients, intensities, new double[]{max, b0});
        double[] p = f.params(), s = f.stdErrors();
        return new Result(p[0], p[1], p[1] / factor(), s[0], s[1], s[1] / factor(), f);
    }
    // ------------------------------------------------------------------
    // Variante: o que varia entre os scans e a DURACAO do pulso de gradiente (delta_i),
    // com G e Delta fixos. I(delta) = I0 exp(-b delta^2 (Delta - delta/3)), b = D gamma^2 G^2.
    // INFERENCIA dos dados de referencia (delta_i = p2 + i*inc, Delta = tau); confirmar no
    // notebook de difusao do pynmred.
    // ------------------------------------------------------------------

    /** Modelo em delta para Delta fixo. */
    public static ModelFunction durationModel(double bigDelta) {
        return new ModelFunction() {
            public int numParameters() { return 2; }
            private double x(double dl) { return dl * dl * (bigDelta - dl / 3.0); }
            public double value(double dl, double[] p) { return p[0] * Math.exp(-p[1] * x(dl)); }
            public double[] gradient(double dl, double[] p) {
                double e = Math.exp(-p[1] * x(dl));
                return new double[]{e, -p[0] * x(dl) * e};
            }
        };
    }

    /**
     * Ajuste com delta variavel. {@code gradient} em T/m (NaN/<=0 se desconhecido: D = NaN,
     * e so b e reportado). D = b / (gamma^2 G^2) [m^2/s].
     */
    public static Result fitDuration(double[] delta, double[] intensities, double bigDelta,
                                     double gamma, double gradient) {
        if (bigDelta <= 0) throw new IllegalArgumentException("Delta deve ser positivo");
        for (double d : delta)
            if (!(d > 0) || d >= 3 * bigDelta)
                throw new IllegalArgumentException("delta invalido (exige 0 < delta < 3*Delta): " + d);
        ModelFunction m = durationModel(bigDelta);
        double max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY, xmin = Double.POSITIVE_INFINITY, xmax = 0;
        for (double v : intensities) { max = Math.max(max, v); min = Math.min(min, v); }
        for (double d : delta) { double x = d * d * (bigDelta - d / 3); xmin = Math.min(xmin, x); xmax = Math.max(xmax, x); }
        double b0 = (min > 0 && max > min && xmax > xmin) ? Math.log(max / min) / (xmax - xmin) : 1.0 / xmax;
        FitResult f = new LevenbergMarquardt().fit(m, delta, intensities, new double[]{max, b0});
        double[] p = f.params(), s = f.stdErrors();
        double k = (gradient > 0 && Double.isFinite(gradient)) ? gamma * gamma * gradient * gradient : Double.NaN;
        return new Result(p[0], p[1], p[1] / k, s[0], s[1], s[1] / k, f);
    }
}
