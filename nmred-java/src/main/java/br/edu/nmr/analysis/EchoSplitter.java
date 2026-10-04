package br.edu.nmr.analysis;

/**
 * Separacao dos ecos de um traco CPMG (T2). Echo k = amostras
 * [round((soff + k*step)*sr), +round(slen*sr)). Parametros vindos do JSON:
 * soff, slen (pproc) e step = pinc['inc'][0]. Inferido dos dados de referencia:
 * com soff=0.25, slen=0.25, inc=0.25 e 12 ecos a ultima janela termina exatamente em
 * 3.25 s = nsamp/sr, e cada janela contem um eco.
 */
public final class EchoSplitter {
    public static double[][] split(double[] trace, double sr, double soff, double slen, double step, int nEchoes) {
        if (sr <= 0 || slen <= 0 || step <= 0 || soff < 0 || nEchoes <= 0)
            throw new IllegalArgumentException("Parametros de separacao invalidos");
        int len = (int) Math.round(slen * sr);
        double[][] out = new double[nEchoes][];
        for (int k = 0; k < nEchoes; k++) {
            int i1 = (int) Math.round((soff + k * step) * sr);
            int i2 = i1 + len;
            if (i1 < 0 || i2 > trace.length)
                throw new IllegalArgumentException("Eco " + k + " fora do traco: [" + i1 + ", " + i2 + ") de " + trace.length);
            out[k] = java.util.Arrays.copyOfRange(trace, i1, i2);
        }
        return out;
    }

    /**
     * Separacao identica a {@code expbase.split()} do pynmred (confirmada no codigo original):
     * ioff = round(soff*sr), ilen = round(slen*sr), janelas CONSECUTIVAS de tamanho ilen
     * (o passo e slen, nao pinc['inc']) e n = (len - ioff) / ilen (divisao inteira).
     * Equivale a {@link #split} apenas quando inc == slen (caso dos dados de referencia).
     */
    public static double[][] splitLikePynmred(double[] trace, double sr, double soff, double slen) {
        if (sr <= 0 || slen <= 0 || soff < 0)
            throw new IllegalArgumentException("Parametros de separacao invalidos");
        int ioff = (int) Math.round(soff * sr), ilen = (int) Math.round(slen * sr);
        if (ilen <= 0 || ioff >= trace.length)
            throw new IllegalArgumentException("Janela de eco invalida: ioff=" + ioff + ", ilen=" + ilen);
        int n = (trace.length - ioff) / ilen;
        double[][] out = new double[n][];
        for (int k = 0; k < n; k++)
            out[k] = java.util.Arrays.copyOfRange(trace, ioff + k * ilen, ioff + (k + 1) * ilen);
        return out;
    }
    private EchoSplitter() {}
}
