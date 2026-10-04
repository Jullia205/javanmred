package br.edu.nmr.analysis;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Gera em Markdown a tabela Python x Java (erro absoluto, erro relativo, tolerancia) da Etapa 15,
 * a partir das integrais e dos parametros de referencia do scipy em src/test/resources.
 * Uso: java -cp target/classes br.edu.nmr.analysis.ValidationReport src/test/resources
 */
public final class ValidationReport {
    private record Row(String name, double py, double java, double tol) {}

    public static void main(String[] a) throws Exception {
        Path res = Path.of(a.length > 0 ? a[0] : "src/test/resources");
        StringBuilder md = new StringBuilder();
        RelaxationFit rf = new RelaxationFit();

        double[][] t1 = csv(res.resolve("t1_integrais_python.csv")); var r1 = rf.fitT1(t1[0], t1[1]);
        String j1 = read(res, "t1_ref.json");
        table(md, "T1 (recuperacao, y = A exp(-R t) + C)", List.of(
            new Row("A", num(j1, "A"), r1.a(), 1e-5), new Row("R1 (s^-1)", num(j1, "R"), r1.r(), 1e-5),
            new Row("C", num(j1, "C"), r1.c(), 1e-5), new Row("sigma(R1)", num(j1, "sR"), r1.sigmaR(), 1e-5),
            new Row("T1 = 1/R1 (s)", 1 / num(j1, "R"), r1.t(), 1e-5)), r1.fit());

        double[][] t2 = csv(res.resolve("t2_integrais_python.csv")); var r2 = rf.fitT2(t2[0], t2[1]);
        String j2 = read(res, "t2_ref.json");
        table(md, "T2 (decaimento dos ecos, y = A exp(-R t) + C)", List.of(
            new Row("A", num(j2, "A"), r2.a(), 1e-5), new Row("R2 (s^-1)", num(j2, "R"), r2.r(), 1e-5),
            new Row("C", num(j2, "C"), r2.c(), 1e-5), new Row("sigma(R2)", num(j2, "sR"), r2.sigmaR(), 1e-5),
            new Row("T2 = 1/R2 (s)", 1 / num(j2, "R"), r2.t(), 1e-5)), r2.fit());

        double[][] d = csv(res.resolve("difusao_integrais_python.csv"));
        var rd = new DiffusionFit(DiffusionFit.GAMMA_1H, 0.004, 0.08).fit(d[0], d[1]);
        String jd = read(res, "difusao_ref.json");
        String tight = jd.substring(jd.indexOf("tight"));
        table(md, "Difusao, modelo em G (I = I0 exp(-b G^2), G = i x inc) - contra curve_fit padrao", List.of(
            new Row("I0", num(jd, "I0"), rd.i0(), 1e-4), new Row("b", num(jd, "b"), rd.b(), 1e-4),
            new Row("sigma(b)", num(jd, "sb"), rd.sigmaB(), 1e-4)), rd.fit());
        table(md, "Difusao, modelo em G - contra scipy com ftol=xtol=gtol=1e-15", List.of(
            new Row("I0", num(tight, "I0"), rd.i0(), 1e-6), new Row("b", num(tight, "b"), rd.b(), 1e-6),
            new Row("sigma(b)", num(tight, "sb"), rd.sigmaB(), 1e-6)), null);

        double[][] dd = csv(res.resolve("difusao_delta_integrais_python.csv"));
        var rdd = DiffusionFit.fitDuration(dd[0], dd[1], 0.08, DiffusionFit.GAMMA_1H, Double.NaN);
        String je = read(res, "difusao_delta_ref.json");
        table(md, "Difusao, delta variavel (I = I0 exp(-b delta^2 (Delta - delta/3)), delta = p2 + i x inc) - curve_fit padrao", List.of(
            new Row("I0", num(je, "I0"), rdd.i0(), 1e-4), new Row("b", num(je, "b"), rdd.b(), 1e-4),
            new Row("sigma(b)", num(je, "sb"), rdd.sigmaB(), 1e-4)), rdd.fit());
        System.out.print(md);
    }

    private static void table(StringBuilder md, String title, List<Row> rows, FitResult fit) {
        md.append("### ").append(title).append("\n\n");
        md.append("| Grandeza | Python | Java | Erro absoluto | Erro relativo | Tolerancia (rel.) | Resultado |\n|---|---|---|---|---|---|---|\n");
        for (Row r : rows) {
            double abs = Math.abs(r.py - r.java), rel = abs / Math.abs(r.py);
            md.append(String.format(Locale.ROOT, "| %s | %.10g | %.10g | %.2e | %.2e | %.0e | %s |%n",
                r.name, r.py, r.java, abs, rel, r.tol, rel <= r.tol ? "OK" : "FALHA"));
        }
        if (fit != null) {
            double maxRes = 0;
            for (double v : fit.residuals()) maxRes = Math.max(maxRes, Math.abs(v));
            md.append(String.format(Locale.ROOT, "%nJava: R^2 = %.6f, RSS = %.4e, max|residuo| = %.3e, iteracoes = %d, convergiu = %s.%n",
                fit.rSquared(), fit.rss(), maxRes, fit.iterations(), fit.converged()));
        }
        md.append("\n");
    }

    private static String read(Path res, String f) throws Exception { return Files.readString(res.resolve(f)); }

    private static double num(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\": *(-?[0-9.eE+-]+)").matcher(json);
        if (!m.find()) throw new IllegalStateException(key + " ausente");
        return Double.parseDouble(m.group(1));
    }

    private static double[][] csv(Path p) throws Exception {
        List<double[]> rows = new ArrayList<>();
        for (String l : Files.readAllLines(p)) {
            if (l.startsWith("#") || l.isBlank()) continue;
            String[] s = l.split(",");
            rows.add(new double[]{Double.parseDouble(s[0]), Double.parseDouble(s[1])});
        }
        return new double[][]{rows.stream().mapToDouble(r -> r[0]).toArray(), rows.stream().mapToDouble(r -> r[1]).toArray()};
    }
    private ValidationReport() {}
}
