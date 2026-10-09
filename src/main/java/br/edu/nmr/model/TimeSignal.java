package br.edu.nmr.model;

/**
 * Um traço de sinal no domínio do tempo — equivalente ao {@code timedata} do
 * Python (nmrbase/expdta.py).
 *
 * <p>Guarda uma matriz [canal][amostra], o intervalo entre amostras
 * {@code dx} (= 1/srate, em segundos) e o instante inicial {@code x0}
 * (normalmente 0 na leitura crua). A responsabilidade desta classe é
 * SÓ representar o dado — filtro digital, janelas, FFT etc. (funções
 * {@code butter}, {@code ft}, ... no Python) ficam para os pacotes
 * {@code signal/} e {@code analysis/} de outra parte da equipe.</p>
 */
public final class TimeSignal {

    private final double[][] samples; // [canal][amostra]
    private final double dx;          // intervalo de amostragem, em segundos
    private final double x0;          // tempo inicial, em segundos

    public TimeSignal(double[][] samples, double dx, double x0) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("TimeSignal precisa de ao menos 1 canal de dados");
        }
        int len = samples[0].length;
        for (double[] channel : samples) {
            if (channel.length != len) {
                throw new IllegalArgumentException(
                    "Todos os canais de um TimeSignal devem ter o mesmo número de amostras");
            }
        }
        if (dx <= 0) {
            throw new IllegalArgumentException("dx (intervalo de amostragem) deve ser positivo, veio: " + dx);
        }
        this.samples = samples;
        this.dx = dx;
        this.x0 = x0;
    }

    public int numChannels() {
        return samples.length;
    }

    public int numSamples() {
        return samples.length == 0 ? 0 : samples[0].length;
    }

    public double dx() {
        return dx;
    }

    public double x0() {
        return x0;
    }

    public double durationSeconds() {
        return numSamples() * dx;
    }

    public double[] channel(int index) {
        return samples[index].clone();
    }

    public double[] timeAxis() {
        int n = numSamples();
        double[] t = new double[n];
        for (int i = 0; i < n; i++) {
            t[i] = x0 + i * dx;
        }
        return t;
    }

    public int timeToIndex(double t) {
        int i = (int) Math.round((t - x0) / dx);
        if (i < 0) return 0;
        if (i > numSamples() - 1) return Math.max(0, numSamples() - 1);
        return i;
    }

    public long countNonFiniteValues() {
        long count = 0;
        for (double[] channel : samples) {
            for (double v : channel) {
                if (!Double.isFinite(v)) count++;
            }
        }
        return count;
    }
}
