package br.edu.nmr.ui;

import br.edu.nmr.analysis.*;

import java.io.File;
import java.nio.file.*;
import java.util.*;

/**
 * CLI: gera os PNGs de ajuste a partir das integrais de referencia (t1/t2/difusao).
 * Uso: java -cp target/classes br.edu.nmr.ui.GenerateFitPlots pasta_resources pasta_saida
 */
public final class GenerateFitPlots {
    public static void main(String[] a) throws Exception {
        if (a.length != 2) { System.err.println("Uso: GenerateFitPlots <pasta_resources> <pasta_saida>"); System.exit(1); }
        Path res = Path.of(a[0]); File out = new File(a[1]);
        RelaxationFit rf = new RelaxationFit();

        double[][] t1 = csv(res.resolve("t1_integrais_python.csv"));
        var r1 = rf.fitT1(t1[0], t1[1]);
        new FitPlot("T1 - recuperacao: R1 = " + String.format(Locale.ROOT, "%.4f +- %.4f s^-1", r1.r(), r1.sigmaR()),
                "tempo de polarizacao (s)", "integral do pico (u.a.)")
            .data(t1[0], t1[1]).fit(RelaxationFit.MODEL, r1.fit())
            .reference(new double[]{ref(res, "t1_ref.json", "A"), ref(res, "t1_ref.json", "R"), ref(res, "t1_ref.json", "C")}, "scipy (Python)")
            .save(new File(out, "T1_ajuste.png"), 900, 720);

        double[][] t2 = csv(res.resolve("t2_integrais_python.csv"));
        var r2 = rf.fitT2(t2[0], t2[1]);
        new FitPlot("T2 - decaimento dos ecos: R2 = " + String.format(Locale.ROOT, "%.4f +- %.4f s^-1", r2.r(), r2.sigmaR()),
                "tempo do eco (s)", "integral do eco (u.a.)")
            .data(t2[0], t2[1]).fit(RelaxationFit.MODEL, r2.fit())
            .reference(new double[]{ref(res, "t2_ref.json", "A"), ref(res, "t2_ref.json", "R"), ref(res, "t2_ref.json", "C")}, "scipy (Python)")
            .save(new File(out, "T2_ajuste.png"), 900, 720);

        double[][] d = csv(res.resolve("difusao_integrais_python.csv"));
        var rd = new DiffusionFit(DiffusionFit.GAMMA_1H, 0.004, 0.08).fit(d[0], d[1]);
        new FitPlot("Difusao, modelo em G (rejeitado: residuo sistematico) b = " + String.format(Locale.ROOT, "%.0f +- %.0f", rd.b(), rd.sigmaB()),
                "G = i x inc (unid. do JSON)", "integral do pico (u.a.)")
            .data(d[0], d[1]).fit(DiffusionFit.MODEL, rd.fit())
            .reference(new double[]{ref(res, "difusao_ref.json", "I0"), ref(res, "difusao_ref.json", "b")}, "scipy (Python)")
            .save(new File(out, "Difusao_G2_ajuste.png"), 900, 720);

        double[][] dd = csv(res.resolve("difusao_delta_integrais_python.csv"));
        var rdd = DiffusionFit.fitDuration(dd[0], dd[1], 0.08, DiffusionFit.GAMMA_1H, Double.NaN);
        new FitPlot("Difusao, delta variavel (inferido): b = " + String.format(Locale.ROOT, "%.0f +- %.0f", rdd.b(), rdd.sigmaB()),
                "duracao do gradiente delta = p2 + i x inc (s)", "integral do pico (u.a.)")
            .data(dd[0], dd[1]).fit(DiffusionFit.durationModel(0.08), rdd.fit())
            .reference(new double[]{ref(res, "difusao_delta_ref.json", "I0"), ref(res, "difusao_delta_ref.json", "b")}, "scipy (Python)")
            .save(new File(out, "Difusao_delta_ajuste.png"), 900, 720);
        System.out.println("PNGs gerados em " + out.getAbsolutePath());
    }

    private static double ref(Path res, String file, String key) throws Exception {
        String j = Files.readString(res.resolve(file));
        var m = java.util.regex.Pattern.compile("\"" + key + "\": *(-?[0-9.eE+-]+)").matcher(j);
        if (!m.find()) throw new IllegalStateException(key + " ausente em " + file);
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
    private GenerateFitPlots() {}
}
