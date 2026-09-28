package br.edu.nmr.io;

import br.edu.nmr.model.ExperimentParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ParameterReaderTest {

    private static final String SAMPLE_JSON = """
        [
          {"p1": 0.0015, "frq": 2700.0, "nsamp": 10000, "srate": 44100, "necho": 8, "tau": 0.05, "ppg": "cpmg"},
          {"ftmin": 0.0, "ftmax": 0.25, "intmin": 2500, "intmax": 2900, "wdw": "none"},
          {"nav": 4, "n": [10], "incp": ["ppre.poltime"], "inc": [1.0], "d": 15.0},
          {"poltime": 2.0, "polamp": 5.0},
          {"ninchann": 1, "nsampx": 9980, "sratex": 44098.7}
        ]
        """;

    @Test
    void parsesAllFiveSectionsInOrder(@TempDir Path tmp) throws IOException {
        Path json = tmp.resolve("exp.json");
        Files.writeString(json, SAMPLE_JSON);

        ExperimentParameters params = ParameterReader.read(json);

        assertEquals(0.0015, params.acquisition().requireDouble("p1"), 1e-12);
        assertEquals("cpmg", params.acquisition().requireString("ppg"));
        assertEquals(0.25, params.processing().requireDouble("ftmax"), 1e-12);
        assertEquals(4, params.increment().requireInt("nav"));
        assertEquals(2.0, params.preAcquisition().requireDouble("poltime"), 1e-12);
        assertEquals(1, params.status().requireInt("ninchann"));

        // sratex presente em pstat deve vencer srate de p (mesma regra de expbase.getsrate())
        assertEquals(44098.7, params.effectiveSampleRate(), 1e-6);
    }

    @Test
    void fallsBackToAcquisitionSampleRateWhenStatusHasNone(@TempDir Path tmp) throws IOException {
        String json = """
            [
              {"srate": 1000.0},
              {}, {}, {},
              {"ninchann": 1}
            ]
            """;
        Path jsonPath = tmp.resolve("exp2.json");
        Files.writeString(jsonPath, json);

        ExperimentParameters params = ParameterReader.read(jsonPath);

        assertEquals(1000.0, params.effectiveSampleRate(), 1e-9);
    }

    @Test
    void throwsWhenFileMissing(@TempDir Path tmp) {
        Path missing = tmp.resolve("nope.json");
        assertThrows(DataValidationException.class, () -> ParameterReader.read(missing));
    }

    @Test
    void throwsOnMalformedJson(@TempDir Path tmp) throws IOException {
        Path bad = tmp.resolve("bad.json");
        Files.writeString(bad, "[ { \"a\": } ]");

        assertThrows(DataValidationException.class, () -> ParameterReader.read(bad));
    }

    @Test
    void throwsWhenArrayDoesNotHaveFiveSections(@TempDir Path tmp) throws IOException {
        Path bad = tmp.resolve("bad2.json");
        Files.writeString(bad, "[ {}, {}, {} ]");

        DataValidationException ex = assertThrows(DataValidationException.class,
            () -> ParameterReader.read(bad));
        assertTrue(ex.getMessage().contains("5 seções"));
    }
}
