package br.edu.nmr.ui;

import br.edu.nmr.analysis.FitResult;
import br.edu.nmr.analysis.ModelFunction;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Grafico de ajuste: painel superior = dados + curva ajustada (+ curva de referencia opcional,
 * p.ex. parametros do Python); painel inferior = residuos. Java2D puro, sem dependencias.
 * Funciona em modo headless, entao serve para linha de comando, testes e relatorios.
 */
public final class FitPlot {
    private static final Color DATA = new Color(0x1f77b4), FIT = new Color(0xd62728), REF = new Color(0x2ca02c);
    private final String title, xLabel, yLabel;
    private double[] x, y;
    private ModelFunction model;
    private double[] params, residuals, refParams;
    private String fitLabel = "ajuste (Java)", refLabel = "referencia (Python)";

    public FitPlot(String title, String xLabel, String yLabel) {
        this.title = title; this.xLabel = xLabel; this.yLabel = yLabel;
    }

    public FitPlot data(double[] x, double[] y) { this.x = x; this.y = y; return this; }

    public FitPlot fit(ModelFunction m, FitResult r) {
        this.model = m; this.params = r.params(); this.residuals = r.residuals(); return this;
    }

    /** Curva de referencia (mesmo modelo, outros parametros) sobreposta ao ajuste. */
    public FitPlot reference(double[] refParams, String label) {
        this.refParams = refParams; this.refLabel = label; return this;
    }

    public BufferedImage render(int w, int h) {
        if (x == null || y == null || model == null) throw new IllegalStateException("Defina data() e fit() antes de render()");
        System.setProperty("java.awt.headless", "true");
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);

        double xmin = min(x), xmax = max(x), pad = (xmax - xmin) * 0.03;
        xmin -= pad; xmax += pad;
        int n = 300;
        double[] cx = new double[n], cy = new double[n], ry = new double[n];
        for (int i = 0; i < n; i++) {
            cx[i] = xmin + (xmax - xmin) * i / (n - 1);
            cy[i] = model.value(cx[i], params);
            if (refParams != null) ry[i] = model.value(cx[i], refParams);
        }
        double ymin = Math.min(min(y), min(cy)), ymax = Math.max(max(y), max(cy));
        double yp = (ymax - ymin) * 0.06; ymin -= yp; ymax += yp;

        int left = 80, right = 25, top = 70, gap = 55;
        int plotW = w - left - right, bottom = 60;
        int hTop = (int) ((h - top - bottom - gap) * 0.68), hRes = h - top - bottom - gap - hTop;
        Rectangle pTop = new Rectangle(left, top, plotW, hTop);
        Rectangle pRes = new Rectangle(left, top + hTop + gap, plotW, hRes);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.setColor(Color.BLACK);
        g.drawString(title, left, 26);

        axes(g, pTop, xmin, xmax, ymin, ymax, null, yLabel);
        // curvas
        clip(g, pTop);
        if (refParams != null) line(g, pTop, cx, ry, xmin, xmax, ymin, ymax, REF, new BasicStroke(3.5f));
        line(g, pTop, cx, cy, xmin, xmax, ymin, ymax, FIT, new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, refParams != null ? new float[]{6, 4} : null, 0));
        g.setColor(DATA);
        for (int i = 0; i < x.length; i++) {
            int px = sx(pTop, x[i], xmin, xmax), py = sy(pTop, y[i], ymin, ymax);
            g.fillOval(px - 4, py - 4, 8, 8);
        }
        g.setClip(null);
        legend(g, pTop);

        double rmax = 0;
        for (double r : residuals) rmax = Math.max(rmax, Math.abs(r));
        if (rmax == 0) rmax = 1;
        rmax *= 1.25;
        axes(g, pRes, xmin, xmax, -rmax, rmax, xLabel, "residuo");
        int zero = sy(pRes, 0, -rmax, rmax);
        g.setColor(Color.GRAY); g.drawLine(pRes.x, zero, pRes.x + pRes.width, zero);
        g.setColor(DATA);
        for (int i = 0; i < x.length; i++) {
            int px = sx(pRes, x[i], xmin, xmax), py = sy(pRes, residuals[i], -rmax, rmax);
            g.drawLine(px, zero, px, py); g.fillOval(px - 3, py - 3, 6, 6);
        }
        g.dispose();
        return img;
    }

    public void save(File png, int w, int h) throws IOException {
        File parent = png.getAbsoluteFile().getParentFile();
        if (parent != null) parent.mkdirs();
        ImageIO.write(render(w, h), "png", png);
    }

    // ---- desenho -------------------------------------------------------------
    private void legend(Graphics2D g, Rectangle p) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        int lx = p.x, ly = p.y - 12;
        g.setColor(DATA); g.fillOval(lx, ly - 4, 8, 8); g.setColor(Color.BLACK); g.drawString("dados", lx + 14, ly + 4);
        lx += 70; g.setColor(FIT); g.setStroke(new BasicStroke(1.8f)); g.drawLine(lx, ly, lx + 18, ly); g.setColor(Color.BLACK); g.drawString(fitLabel, lx + 24, ly + 4);
        lx += 24 + g.getFontMetrics().stringWidth(fitLabel) + 22;
        if (refParams != null) { g.setColor(REF); g.setStroke(new BasicStroke(3.5f)); g.drawLine(lx, ly, lx + 18, ly); g.setColor(Color.BLACK); g.drawString(refLabel, lx + 24, ly + 4); }
        g.setStroke(new BasicStroke(1f));
    }

    private void axes(Graphics2D g, Rectangle p, double x0, double x1, double y0, double y1, String xl, String yl) {
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        double[] xt = ticks(x0, x1, 8), yt = ticks(y0, y1, 6);
        for (double t : yt) {
            int py = sy(p, t, y0, y1);
            g.setColor(new Color(0xe5e5e5)); g.drawLine(p.x, py, p.x + p.width, py);
            g.setColor(Color.BLACK); String s = fmt(t); g.drawString(s, p.x - 8 - g.getFontMetrics().stringWidth(s), py + 4);
        }
        for (double t : xt) {
            int px = sx(p, t, x0, x1);
            g.setColor(new Color(0xe5e5e5)); g.drawLine(px, p.y, px, p.y + p.height);
            if (xl != null) { g.setColor(Color.BLACK); String s = fmt(t); g.drawString(s, px - g.getFontMetrics().stringWidth(s) / 2, p.y + p.height + 18); }
        }
        g.setColor(Color.BLACK); g.drawRect(p.x, p.y, p.width, p.height);
        if (xl != null) g.drawString(xl, p.x + p.width / 2 - g.getFontMetrics().stringWidth(xl) / 2, p.y + p.height + 40);
        Graphics2D r = (Graphics2D) g.create();
        r.rotate(-Math.PI / 2, 20, p.y + p.height / 2);
        r.drawString(yl, 20 - r.getFontMetrics().stringWidth(yl) / 2, p.y + p.height / 2);
        r.dispose();
    }

    private static void clip(Graphics2D g, Rectangle p) { g.setClip(p); }

    private static void line(Graphics2D g, Rectangle p, double[] cx, double[] cy, double x0, double x1, double y0, double y1, Color c, Stroke s) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < cx.length; i++) {
            int px = sx(p, cx[i], x0, x1), py = sy(p, cy[i], y0, y1);
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        g.setColor(c); g.setStroke(s); g.draw(path); g.setStroke(new BasicStroke(1f));
    }

    private static int sx(Rectangle p, double v, double a, double b) { return p.x + (int) Math.round((v - a) / (b - a) * p.width); }
    private static int sy(Rectangle p, double v, double a, double b) { return p.y + p.height - (int) Math.round((v - a) / (b - a) * p.height); }

    static double[] ticks(double a, double b, int target) {
        double raw = (b - a) / target, mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double step = mag; double r = raw / mag;
        if (r > 5) step = 10 * mag; else if (r > 2) step = 5 * mag; else if (r > 1) step = 2 * mag;
        double start = Math.ceil(a / step) * step;
        int cnt = (int) Math.floor((b - start) / step + 1e-9) + 1;
        double[] t = new double[Math.max(cnt, 0)];
        for (int i = 0; i < t.length; i++) t[i] = start + i * step;
        return t;
    }

    private static String fmt(double v) {
        if (v == 0) return "0";
        double a = Math.abs(v);
        if (a >= 1e4 || a < 1e-3) return String.format(Locale.ROOT, "%.1e", v);
        String t = new java.math.BigDecimal(String.format(Locale.ROOT, "%.6g", v)).stripTrailingZeros().toPlainString();
        return t;
    }

    private static double min(double[] a) { double m = Double.POSITIVE_INFINITY; for (double v : a) m = Math.min(m, v); return m; }
    private static double max(double[] a) { double m = Double.NEGATIVE_INFINITY; for (double v : a) m = Math.max(m, v); return m; }
}
