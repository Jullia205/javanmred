package br.edu.nmr.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BinarySignalReaderTest {

    @Test
    void readsKnownLittleEndianValues(@TempDir Path tmp) throws IOException {
        double[] expected = {0.0, 1.5, -2.25, 100.0, -0.001};
        Path bin = tmp.resolve("trace.bin");
        writeFloat64LE(bin, expected);

        double[] actual = BinarySignalReader.readFloat64(bin);

        assertArrayEquals(expected, actual, 1e-12);
    }

    @Test
    void throwsWithFileNameWhenFileMissing(@TempDir Path tmp) {
        Path missing = tmp.resolve("does_not_exist.bin");

        DataValidationException ex = assertThrows(DataValidationException.class,
            () -> BinarySignalReader.readFloat64(missing));

        assertTrue(ex.getMessage().contains(missing.toString()));
        assertTrue(ex.getMessage().toLowerCase().contains("não encontrado"));
    }

    @Test
    void throwsWhenFileSizeIsNotMultipleOf8Bytes(@TempDir Path tmp) throws IOException {
        Path bin = tmp.resolve("corrupted.bin");
        Files.write(bin, new byte[]{1, 2, 3}); // 3 bytes, not a multiple of 8

        DataValidationException ex = assertThrows(DataValidationException.class,
            () -> BinarySignalReader.readFloat64(bin));

        assertTrue(ex.getMessage().contains("múltiplo"));
    }

    @Test
    void throwsWhenFileIsEmpty(@TempDir Path tmp) throws IOException {
        Path bin = tmp.resolve("empty.bin");
        Files.write(bin, new byte[0]);

        assertThrows(DataValidationException.class, () -> BinarySignalReader.readFloat64(bin));
    }

    private static void writeFloat64LE(Path path, double[] values) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : values) {
            buffer.putDouble(v);
        }
        Files.write(path, buffer.array());
    }
}
