package br.edu.nmr.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class FitTest {

    @Test
    void t1SinteticoRecuperaParametros() {
        double[] t = new double[11], y = new double[11];
        for (int i = 0; i < 11; i++) { t[i] = i; y[i] = -1.3 * Math.exp(-0.4 * i) + 1.3; }
        var r = new RelaxationFit().fitT1(t, y);
        assertEquals(0.4, r.r(), 1e-6);
        assertEquals(2.5, r.t(), 1e-5);
        assertEquals(-1.3, r.a(), 1e-6);
        assertEquals(1.3, r.c(), 1e-6);
        assertTrue(r.fit().converged());
    }

    @Test
    void t2SinteticoComRuido() {
        Random rnd = new Random(42);
        double[] t = new double[12], y = new double[12];
        for (int i = 0; i < 12; i++) { t[i] = 0.25 * (i + 1); y[i] = 2.0 * Math.exp(-t[i] / 1.2) + 0.01 * rnd.nextGaussian(); }
        var r = new RelaxationFit().fitT2(t, y);
        assertEquals(1.2, r.t(), 0.1);
        assertTrue(r.fit().rSquared() > 0.99);
    }

    @Test
    void difusaoRecuperaD() {
        double D = 2.3e-9, delta = 0.004, Delta = 0.08;
        var fit = new DiffusionFit(DiffusionFit.GAMMA_1H, delta, Delta);
        double[] g = new double[12], y = new double[12];
        for (int i = 0; i < 12; i++) { g[i] = 0.0005 * (i + 1); y[i] = 5.0 * Math.exp(-D * fit.factor() * g[i] * g[i]); }
        var r = fit.fit(g, y);
        assertEquals(D, r.d(), D * 1e-5);
        assertEquals(5.0, r.i0(), 1e-5);
    }

    @Test
    void equivalenciaComScipyT1() throws Exception {
        List<double[]> rows = new ArrayList<>();
        for (String l : Files.readAllLines(Path.of("src/test/resources/t1_integrais_python.csv"))) {
            if (l.startsWith("#") || l.isBlank()) continue;
            String[] s = l.split(",");
            rows.add(new double[]{Double.parseDouble(s[0]), Double.parseDouble(s[1])});
        }
        double[] t = rows.stream().mapToDouble(r -> r[0]).toArray();
        double[] y = rows.stream().mapToDouble(r -> r[1]).toArray();
        var r = new RelaxationFit().fitT1(t, y);
        // scipy.curve_fit: A=-1.26420382 R=0.38743462 C=1.28497352; sigmaR=0.00375535
        assertEquals(0.38743462, r.r(), 1e-6);
        assertEquals(-1.26420382, r.a(), 1e-6);
        assertEquals(1.28497352, r.c(), 1e-6);
        assertEquals(0.00375535, r.sigmaR(), 1e-6);
    }

    @Test
    void entradasInvalidas() {
        var rf = new RelaxationFit();
        assertThrows(IllegalArgumentException.class, () -> rf.fitT1(new double[]{1, 2}, new double[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> rf.fitT1(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class,
            () -> rf.fitT1(new double[]{1, 2, 3, 4}, new double[]{1, Double.NaN, 3, 4}));
        assertThrows(IllegalArgumentException.class, () -> new DiffusionFit(1, 0.1, 0.01));
    }

    @Test
    void integradorSomaEUnidade() {
        double[] f = {0, 10, 20, 30, 40}, m = {1, 2, 3, 4, 5};
        assertEquals(5.0, PeakIntegrator.sumBins(f, m, 10, 30), 1e-12);            // bins 10 e 20
        assertEquals(50.0, PeakIntegrator.integralWithUnit(f, m, 10, 30), 1e-12);  // x delta f = 10
        assertThrows(IllegalArgumentException.class, () -> PeakIntegrator.sumBins(f, m, 100, 200));
    }
}
