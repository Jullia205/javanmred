package br.edu.nmr.analysis;

/**
 * Levenberg-Marquardt de minimos quadrados (mesma familia do metodo 'lm' do
 * scipy.optimize.curve_fit). Java puro, sem dependencias.
 * Covariancia = (J^T J)^-1 * rss/(n-p), como no curve_fit (absolute_sigma=False).
 */
public final class LevenbergMarquardt {
    private final int maxIterations;
    private final double tol;

    public LevenbergMarquardt() { this(10000, 1e-12); }
    public LevenbergMarquardt(int maxIterations, double tol) {
        this.maxIterations = maxIterations;
        this.tol = tol;
    }

    public FitResult fit(ModelFunction m, double[] x, double[] y, double[] p0) {
        int n = x.length, k = m.numParameters();
        if (y.length != n) throw new IllegalArgumentException("x e y com tamanhos diferentes: " + n + " vs " + y.length);
        if (p0.length != k) throw new IllegalArgumentException("p0 deve ter " + k + " valores, veio " + p0.length);
        if (n <= k) throw new IllegalArgumentException("Pontos insuficientes: " + n + " para " + k + " parametros");
        for (int i = 0; i < n; i++)
            if (!Double.isFinite(x[i]) || !Double.isFinite(y[i]))
                throw new IllegalArgumentException("Valor nao finito no ponto " + i);

        double[] p = p0.clone();
        double lambda = 1e-3;
        double cost = rss(m, x, y, p);
        int it = 0;
        boolean converged = false;
        while (it < maxIterations) {
            it++;
            double[][] jtj = new double[k][k];
            double[] jtr = new double[k];
            for (int i = 0; i < n; i++) {
                double r = y[i] - m.value(x[i], p);
                double[] g = m.gradient(x[i], p);
                for (int a = 0; a < k; a++) {
                    jtr[a] += g[a] * r;
                    for (int b = 0; b < k; b++) jtj[a][b] += g[a] * g[b];
                }
            }
            boolean improved = false;
            for (int tries = 0; tries < 50; tries++) {
                double[][] A = new double[k][k];
                for (int a = 0; a < k; a++) {
                    System.arraycopy(jtj[a], 0, A[a], 0, k);
                    A[a][a] += lambda * Math.max(jtj[a][a], 1e-30);
                }
                double[] step = solve(A, jtr);
                if (step == null) { lambda *= 10; continue; }
                double[] pn = new double[k];
                for (int a = 0; a < k; a++) pn[a] = p[a] + step[a];
                double cn = rss(m, x, y, pn);
                if (Double.isFinite(cn) && cn <= cost) {
                    double rel = (cost - cn) / Math.max(cost, 1e-300);
                    double maxStep = 0;
                    for (int a = 0; a < k; a++)
                        maxStep = Math.max(maxStep, Math.abs(step[a]) / Math.max(Math.abs(p[a]), 1e-12));
                    p = pn; cost = cn; lambda = Math.max(lambda / 10, 1e-15);
                    improved = true;
                    if (rel < tol && maxStep < 1e-8) converged = true;
                    break;
                }
                lambda *= 10;
            }
            if (!improved) { converged = true; break; }  // sem melhora possivel: minimo local
            if (converged) break;
        }

        double[] res = new double[n];
        double mean = 0;
        for (double v : y) mean += v;
        mean /= n;
        double sst = 0;
        for (int i = 0; i < n; i++) {
            res[i] = y[i] - m.value(x[i], p);
            sst += (y[i] - mean) * (y[i] - mean);
        }
        double[][] cov = covariance(m, x, p, cost, n);
        double[] se = new double[k];
        for (int a = 0; a < k; a++) se[a] = cov == null ? Double.NaN : Math.sqrt(Math.max(cov[a][a], 0));
        return new FitResult(p, se, cov, res, cost, sst > 0 ? 1 - cost / sst : Double.NaN, it, converged);
    }

    private static double rss(ModelFunction m, double[] x, double[] y, double[] p) {
        double s = 0;
        for (int i = 0; i < x.length; i++) { double r = y[i] - m.value(x[i], p); s += r * r; }
        return s;
    }

    private static double[][] covariance(ModelFunction m, double[] x, double[] p, double rss, int n) {
        int k = p.length;
        double[][] jtj = new double[k][k];
        for (double xi : x) {
            double[] g = m.gradient(xi, p);
            for (int a = 0; a < k; a++) for (int b = 0; b < k; b++) jtj[a][b] += g[a] * g[b];
        }
        double[][] inv = invert(jtj);
        if (inv == null) return null;
        double s2 = rss / (n - k);
        for (double[] row : inv) for (int b = 0; b < k; b++) row[b] *= s2;
        return inv;
    }

    /** Resolve A x = b por eliminacao de Gauss com pivoteamento parcial; null se singular. */
    static double[] solve(double[][] A, double[] b) {
        int k = b.length;
        double[][] M = new double[k][k + 1];
        for (int i = 0; i < k; i++) { System.arraycopy(A[i], 0, M[i], 0, k); M[i][k] = b[i]; }
        for (int c = 0; c < k; c++) {
            int piv = c;
            for (int r = c + 1; r < k; r++) if (Math.abs(M[r][c]) > Math.abs(M[piv][c])) piv = r;
            if (Math.abs(M[piv][c]) < 1e-300) return null;
            double[] t = M[c]; M[c] = M[piv]; M[piv] = t;
            for (int r = c + 1; r < k; r++) {
                double f = M[r][c] / M[c][c];
                for (int j = c; j <= k; j++) M[r][j] -= f * M[c][j];
            }
        }
        double[] x = new double[k];
        for (int i = k - 1; i >= 0; i--) {
            double s = M[i][k];
            for (int j = i + 1; j < k; j++) s -= M[i][j] * x[j];
            x[i] = s / M[i][i];
        }
        return x;
    }

    static double[][] invert(double[][] A) {
        int k = A.length;
        double[][] inv = new double[k][k];
        for (int c = 0; c < k; c++) {
            double[] e = new double[k]; e[c] = 1;
            double[] col = solve(A, e);
            if (col == null) return null;
            for (int r = 0; r < k; r++) inv[r][c] = col[r];
        }
        return inv;
    }
}
