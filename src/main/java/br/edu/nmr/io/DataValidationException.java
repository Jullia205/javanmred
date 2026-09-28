package br.edu.nmr.io;

/**
 * Erro de leitura/validação de dados de um experimento.
 *
 * O roteiro (Etapa 7) exige explicitamente que "mensagens de erro
 * indiquem o arquivo e a causa da falha" — por isso esta exceção sempre
 * carrega o caminho do arquivo problemático separado da causa, e monta
 * a mensagem final de forma consistente.
 */
public class DataValidationException extends RuntimeException {

    private final String filePath;

    public DataValidationException(String filePath, String reason) {
        super(formatMessage(filePath, reason));
        this.filePath = filePath;
    }

    public DataValidationException(String filePath, String reason, Throwable cause) {
        super(formatMessage(filePath, reason), cause);
        this.filePath = filePath;
    }

    public String filePath() {
        return filePath;
    }

    private static String formatMessage(String filePath, String reason) {
        return "Falha ao ler/validar '" + filePath + "': " + reason;
    }
}
