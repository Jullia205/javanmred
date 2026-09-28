package br.edu.nmr.io;

import br.edu.nmr.model.Experiment;
import br.edu.nmr.model.TimeSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração: escreve um par .json + .bin sintético (2 canais,
 * 3 amostras/canal, 2 scans) e confere se o ExperimentReader reconstrói
 * exatamente os mesmos números, na mesma ordem que o numpy reshape([nchann,nsamp])
 * do Python produziria.
 */
class ExperimentReaderTest {

    private static final String JSON_2CH_3SAMP = """
        [
          {"nsamp": 3, "srate": 1000.0},
          {},
          {},
          {},
          {"ninchann": 2, "nsampx": 3, "sratex": 1000.0}
        ]
        """;

    @Test
    void loadsTwoScansWithTwoChannelsCorrectly(@TempDir Path tmp) throws IOException {
        Path base = tmp.resolve("exp");
        Files.writeString(tmp.resolve("exp.json"), JSON_2CH_3SAMP);
        // scan0: ch0=[0,1,2] ch1=[3,4,5] | scan1: ch0=[6,7,8] ch1=[9,10,11]
        writeFloat64LE(tmp.resolve("exp.bin"), new double[]{0,1,2,3,4,5,6,7,8,9,10,11});

        Experiment experiment = new ExperimentReader().load(base);

        assertEquals(2, experiment.numScans());

        TimeSignal scan0 = experiment.scan(0);
        assertEquals(2, scan0.numChannels());
        assertEquals(3, scan0.numSamples());
        assertArrayEquals(new double[]{0, 1, 2}, scan0.channel(0), 1e-12);
        assertArrayEquals(new double[]{3, 4, 5}, scan0.channel(1), 1e-12);
        assertEquals(0.001, scan0.dx(), 1e-12);

        TimeSignal scan1 = experiment.scan(1);
        assertArrayEquals(new double[]{6, 7, 8}, scan1.channel(0), 1e-12);
        assertArrayEquals(new double[]{9, 10, 11}, scan1.channel(1), 1e-12);
    }

    @Test
    void acceptsBaseArgumentWithOrWithoutExtension(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("exp.json"), JSON_2CH_3SAMP);
        writeFloat64LE(tmp.resolve("exp.bin"), new double[]{0,1,2,3,4,5});

        Experiment withJsonExt = new ExperimentReader().load(tmp.resolve("exp.json"));
        Experiment withoutExt = new ExperimentReader().load(tmp.resolve("exp"));

        assertEquals(1, withJsonExt.numScans());
        assertEquals(1, withoutExt.numScans());
    }

    @Test
    void throwsWhenFileIsTruncatedBelowOneFullScan(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("exp.json"), JSON_2CH_3SAMP);
        // precisa de 6 valores para 1 scan completo (2 canais x 3 amostras); só dá 4
        writeFloat64LE(tmp.resolve("exp.bin"), new double[]{0, 1, 2, 3});

        assertThrows(DataValidationException.class, () -> new ExperimentReader().load(tmp.resolve("exp")));
    }

    @Test
    void fallsBackToPNsampMinusNskipWhenStatusHasNoNsampx(@TempDir Path tmp) throws IOException {
        String json = """
            [
              {"nsamp": 5, "nskip": 2, "srate": 500.0},
              {}, {}, {},
              {"ninchann": 1}
            ]
            """;
        Files.writeString(tmp.resolve("exp.json"), json);
        // nsamp efetivo = 5 - 2 = 3 amostras/canal
        writeFloat64LE(tmp.resolve("exp.bin"), new double[]{10, 20, 30});

        Experiment experiment = new ExperimentReader().load(tmp.resolve("exp"));

        assertEquals(1, experiment.numScans());
        assertEquals(3, experiment.scan(0).numSamples());
        assertArrayEquals(new double[]{10, 20, 30}, experiment.scan(0).channel(0), 1e-12);
    }

    @Test
    void rejectsNonFiniteSamplesByDefault(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("exp.json"), JSON_2CH_3SAMP);
        writeFloat64LE(tmp.resolve("exp.bin"), new double[]{0, 1, Double.NaN, 3, 4, 5});

        assertThrows(DataValidationException.class, () -> new ExperimentReader().load(tmp.resolve("exp")));
    }

    @Test
    void allowsNonFiniteSamplesWhenConfiguredTo(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("exp.json"), JSON_2CH_3SAMP);
        writeFloat64LE(tmp.resolve("exp.bin"), new double[]{0, 1, Double.NaN, 3, 4, 5});

        ExperimentReader lenientReader = new ExperimentReader(false);
        Experiment experiment = lenientReader.load(tmp.resolve("exp"));

        assertEquals(1, experiment.scan(0).countNonFiniteValues());
    }

    private static void writeFloat64LE(Path path, double[] values) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : values) {
            buffer.putDouble(v);
        }
        Files.write(path, buffer.array());
    }
}
