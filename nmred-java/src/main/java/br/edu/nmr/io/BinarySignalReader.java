package br.edu.nmr.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Leitor de baixo nível para os arquivos .bin do experimento.
 *
 * <p>O Python salva os dados com {@code numpy_array.astype('float64').tofile(file)}
 * (expdta.data.save). {@code numpy.tofile} grava os bytes crus, na ordem de
 * bytes NATIVA da máquina onde o script rodou. Em praticamente qualquer PC
 * de laboratório (Windows/Linux/Mac em x86_64 ou Apple Silicon) isso é
 * little-endian — por isso o padrão aqui é LITTLE_ENDIAN.</p>
 *
 * <p><b>Isso é uma suposição, não uma certeza — valide!</b> A Etapa 7 do
 * roteiro pede exatamente isso: leia um arquivo de referência com os dois
 * programas e compare os primeiros, alguns do meio e os últimos valores de
 * cada traço. Se os valores vierem visivelmente errados (números absurdos,
 * NaN em excesso), troque para BIG_ENDIAN em {@link #readFloat64(Path, ByteOrder)}
 * e teste de novo — é a maneira mais rápida de descobrir isso na prática.</p>
 */
public final class BinarySignalReader {

    private BinarySignalReader() {
    }

    /** Lê o arquivo binário inteiro como um array de double, assumindo little-endian. */
    public static double[] readFloat64(Path path) {
        return readFloat64(path, ByteOrder.LITTLE_ENDIAN);
    }

    public static double[] readFloat64(Path path, ByteOrder byteOrder) {
        if (!Files.exists(path)) {
            throw new DataValidationException(path.toString(), "arquivo binário não encontrado");
        }

        long sizeBytes;
        try {
            sizeBytes = Files.size(path);
        } catch (IOException e) {
            throw new DataValidationException(path.toString(), "não foi possível determinar o tamanho do arquivo", e);
        }

        if (sizeBytes == 0) {
            throw new DataValidationException(path.toString(), "arquivo binário está vazio");
        }
        if (sizeBytes % Double.BYTES != 0) {
            throw new DataValidationException(path.toString(),
                "tamanho do arquivo (" + sizeBytes + " bytes) não é múltiplo de "
                    + Double.BYTES + " bytes — não é um float64 puro, ou está truncado");
        }

        byte[] raw;
        try {
            raw = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new DataValidationException(path.toString(), "erro de I/O ao ler o arquivo", e);
        }

        ByteBuffer buffer = ByteBuffer.wrap(raw).order(byteOrder);
        DoubleBuffer doubles = buffer.asDoubleBuffer();
        double[] out = new double[doubles.remaining()];
        doubles.get(out);
        return out;
    }
}
