package br.edu.nmr.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Equivalencia Java x scipy.curve_fit nas integrais geradas pelo Python (T1, T2, difusao). */
class ReferenceFitTest {
    private static final double TOL_REL = 1e-5;

    private static double[][] csv(String name) throws Exception {
        List<double[]> rows = new ArrayList<>();
        for (String l : Files.readAllLines(Path.of("src/test/resources/" + name))) {
            if (l.startsWith("#") || l.isBlank()) continue;
            String[] s = l.split(",");
            rows.add(new double[]{Double.parseDouble(s[0]), Double.parseDouble(s[1])});
        }
        return new double[][]{rows.stream().mapToDouble(r -> r[0]).toArray(), rows.stream().mapToDouble(r -> r[1]).toArray()};
    }

    private static double num(String json, String key) {
        var m = java.util.regex.Pattern.compile("\"" + key + "\": *(-?[0-9.eE+-]+)").matcher(json);
        assertTrue(m.find(), key);
        return Double.parseDouble(m.group(1));
    }

    private static void close(double exp, double got, String what) {
        close(exp, got, what, TOL_REL);
    }

    private static void close(double exp, double got, String what, double rel) {
        assertEquals(exp, got, Math.abs(exp) * rel + 1e-12, what);
    }

    @Test
    void t1() throws Exception {
        var d = csv("t1_integrais_python.csv");
        var r = new RelaxationFit().fitT1(d[0], d[1]);
        String j = Files.readString(Path.of("src/test/resources/t1_ref.json"));
        close(num(j, "A"), r.a(), "A"); close(num(j, "R"), r.r(), "R"); close(num(j, "C"), r.c(), "C");
        close(num(j, "sR"), r.sigmaR(), "sR");
    }

    @Test
    void t2() throws Exception {
        var d = csv("t2_integrais_python.csv");
        var r = new RelaxationFit().fitT2(d[0], d[1]);
        String j = Files.readString(Path.of("src/test/resources/t2_ref.json"));
        close(num(j, "A"), r.a(), "A"); close(num(j, "R"), r.r(), "R"); close(num(j, "C"), r.c(), "C");
        close(num(j, "sA"), r.sigmaA(), "sA"); close(num(j, "sR"), r.sigmaR(), "sR"); close(num(j, "sC"), r.sigmaC(), "sC");
    }

    @Test
    void difusaoModeloIQuadrado() throws Exception {
        var d = csv("difusao_integrais_python.csv");
        var r = new DiffusionFit(DiffusionFit.GAMMA_1H, 0.004, 0.08).fit(d[0], d[1]);
        String j = Files.readString(Path.of("src/test/resources/difusao_ref.json"));
        // Tolerancia 1e-4: o curve_fit com parametros padrao para ~2e-6 do minimo; o Java coincide
        // (a 2e-9) com o scipy rodado com ftol=xtol=gtol=1e-15 (b = 20033.4483522).
        close(num(j, "I0"), r.i0(), "I0", 1e-4); close(num(j, "b"), r.b(), "b", 1e-4);
        close(num(j, "sb"), r.sigmaB(), "sb", 1e-4);
    }

    @Test
    void separacaoDeEcos() {
        double[] trace = new double[70200];
        for (int i = 0; i < trace.length; i++) trace[i] = i;
        double[][] e = EchoSplitter.split(trace, 21600, 0.25, 0.25, 0.25, 12);
        assertEquals(12, e.length);
        assertEquals(5400, e[0].length);
        assertEquals(5400, e[0][0]);          // 0.25 s * 21600
        assertEquals(10800, e[1][0]);
        assertEquals(70199, e[11][5399]);     // ultima amostra do traco
        assertThrows(IllegalArgumentException.class, () -> EchoSplitter.split(trace, 21600, 0.25, 0.25, 0.25, 13));
    }

    @Test
    void difusaoDuracaoDoGradienteVariavel() throws Exception {
        var d = csv("difusao_delta_integrais_python.csv");
        var r = DiffusionFit.fitDuration(d[0], d[1], 0.08, DiffusionFit.GAMMA_1H, Double.NaN);
        String j = Files.readString(Path.of("src/test/resources/difusao_delta_ref.json"));
        // referencia: curve_fit com p0 equivalente (bloco "default"); tolerancia 1e-4
        double i0 = num(j.substring(j.indexOf("default")), "I0"), b = num(j.substring(j.indexOf("default")), "b");
        double sb = num(j.substring(j.indexOf("default")), "sb");
        close(i0, r.i0(), "I0", 1e-4); close(b, r.b(), "b", 1e-4); close(sb, r.sigmaB(), "sb", 1e-3);
        assertTrue(Double.isNaN(r.d()), "sem G conhecido, D deve ser NaN");
    }

    @Test
    void difusaoDuracaoRecuperaD() {
        double D = 2.3e-9, G = 0.01, Delta = 0.08;
        double[] dl = new double[12], y = new double[12];
        for (int i = 0; i < 12; i++) {
            dl[i] = 0.004 + 0.001 * i;
            y[i] = 3.0 * Math.exp(-D * DiffusionFit.GAMMA_1H * DiffusionFit.GAMMA_1H * G * G * dl[i] * dl[i] * (Delta - dl[i] / 3));
        }
        var r = DiffusionFit.fitDuration(dl, y, Delta, DiffusionFit.GAMMA_1H, G);
        assertEquals(D, r.d(), D * 1e-5);
        assertEquals(3.0, r.i0(), 1e-5);
        assertThrows(IllegalArgumentException.class,
            () -> DiffusionFit.fitDuration(new double[]{-1, 2}, new double[]{1, 1}, 0.08, 1, 1));
    }

    @Test
    void separacaoDeEcosIgualAoPynmred() {
        double[] trace = new double[70200];
        for (int i = 0; i < trace.length; i++) trace[i] = i;
        // dados de referencia: inc == slen -> mesmo resultado do metodo split(...)
        double[][] a = EchoSplitter.splitLikePynmred(trace, 21600, 0.25, 0.25);
        double[][] b = EchoSplitter.split(trace, 21600, 0.25, 0.25, 0.25, 12);
        assertEquals(12, a.length);
        for (int k = 0; k < 12; k++) assertArrayEquals(b[k], a[k]);
        // n = (len - ioff) / ilen: 70200 amostras, ioff = 5400, ilen = 5400 -> 12; sobras sao descartadas
        assertEquals(11, EchoSplitter.splitLikePynmred(new double[70199], 21600, 0.25, 0.25).length);
        // janelas consecutivas de slen, independentes de 'inc' (pynmred: lt = ioff + i*ilen)
        assertEquals(5400 + 3 * 5400, EchoSplitter.splitLikePynmred(trace, 21600, 0.25, 0.25)[3][0]);
        assertThrows(IllegalArgumentException.class, () -> EchoSplitter.splitLikePynmred(trace, 21600, 0.25, 0));
    }
}
