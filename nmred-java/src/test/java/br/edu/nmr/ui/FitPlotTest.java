package br.edu.nmr.ui;

import static org.junit.jupiter.api.Assertions.*;

import br.edu.nmr.analysis.RelaxationFit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class FitPlotTest {
    private static FitPlot plot() {
        double[] t = new double[11], y = new double[11];
        for (int i = 0; i < 11; i++) { t[i] = i; y[i] = -1.3 * Math.exp(-0.4 * i) + 1.3 + 0.002 * (i % 2); }
        var r = new RelaxationFit().fitT1(t, y);
        return new FitPlot("teste", "t (s)", "I").data(t, y).fit(RelaxationFit.MODEL, r.fit());
    }

    @Test
    void renderizaImagemNaoVazia() {
        BufferedImage img = plot().render(800, 600);
        assertEquals(800, img.getWidth());
        assertEquals(600, img.getHeight());
        int nonWhite = 0;
        for (int x = 0; x < img.getWidth(); x += 3)
            for (int yy = 0; yy < img.getHeight(); yy += 3)
                if ((img.getRGB(x, yy) & 0xFFFFFF) != 0xFFFFFF) nonWhite++;
        assertTrue(nonWhite > 500, "imagem parece em branco");
    }

    @Test
    void salvaPng() throws Exception {
        File f = Files.createTempDirectory("fitplot").resolve("saida/t1.png").toFile();
        plot().save(f, 800, 600);
        assertTrue(f.exists() && f.length() > 1000);
    }

    @Test
    void exigeDadosEAjuste() {
        assertThrows(IllegalStateException.class, () -> new FitPlot("a", "b", "c").render(100, 100));
    }

    @Test
    void ticksCobremOIntervalo() {
        double[] t = FitPlot.ticks(0, 10, 5);
        assertTrue(t.length >= 3);
        assertTrue(t[0] >= 0 && t[t.length - 1] <= 10);
    }
}
