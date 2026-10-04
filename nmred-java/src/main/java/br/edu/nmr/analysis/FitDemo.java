package br.edu.nmr.analysis;

import java.nio.file.*;
import java.util.*;

/** CLI: java -cp target/classes br.edu.nmr.analysis.FitDemo t1|t2 arquivo.csv  (colunas: t,I; cabecalho '#' ok) */
public final class FitDemo {
    public static void main(String[] a) throws Exception {
        if (a.length != 2) { System.err.println("Uso: FitDemo t1|t2 arquivo.csv"); System.exit(1); }
        List<double[]> rows = new ArrayList<>();
        for (String l : Files.readAllLines(Path.of(a[1]))) {
            l = l.trim();
            if (l.isEmpty() || l.startsWith("#") || l.startsWith("t")) continue;
            String[] s = l.split("[,;\\s]+");
            rows.add(new double[]{Double.parseDouble(s[0]), Double.parseDouble(s[1])});
        }
        double[] t = rows.stream().mapToDouble(r -> r[0]).toArray();
        double[] y = rows.stream().mapToDouble(r -> r[1]).toArray();
        RelaxationFit rf = new RelaxationFit();
        RelaxationFit.Result r = a[0].equalsIgnoreCase("t1") ? rf.fitT1(t, y) : rf.fitT2(t, y);
        System.out.printf("A = %.8f +- %.8f%nR = %.8f +- %.8f s^-1%nC = %.8f +- %.8f%nT = %.8f +- %.8f s%nR2 = %.8f  iter=%d  convergiu=%b%n",
            r.a(), r.sigmaA(), r.r(), r.sigmaR(), r.c(), r.sigmaC(), r.t(), r.sigmaT(),
            r.fit().rSquared(), r.fit().iterations(), r.fit().converged());
    }
}
