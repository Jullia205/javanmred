package br.edu.nmr.io;

import br.edu.nmr.io.json.MinimalJsonParser;
import br.edu.nmr.model.ExperimentParameters;
import br.edu.nmr.model.ParamMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Lê o arquivo .json de parâmetros e monta um {@link ExperimentParameters}.
 *
 * Equivalente a:
 * <pre>
 *   with open(filename1,"r") as infile:
 *       a = json.load(infile)
 *   (self.p, self.pproc, self.pinc, self.ppre, self.pstat) = a
 * </pre>
 * do Python (expbase.load). O JSON raiz é sempre um ARRAY de 5 objetos,
 * nesta ordem fixa: p, pproc, pinc, ppre, pstat.
 */
public final class ParameterReader {

    private ParameterReader() {
    }

    @SuppressWarnings("unchecked")
    public static ExperimentParameters read(Path jsonPath) {
        if (!Files.exists(jsonPath)) {
            throw new DataValidationException(jsonPath.toString(), "arquivo de parâmetros (.json) não encontrado");
        }

        String content;
        try {
            content = Files.readString(jsonPath);
        } catch (IOException e) {
            throw new DataValidationException(jsonPath.toString(), "erro de I/O ao ler o arquivo", e);
        }

        Object parsed;
        try {
            parsed = MinimalJsonParser.parse(content);
        } catch (MinimalJsonParser.JsonParseException e) {
            throw new DataValidationException(jsonPath.toString(), "JSON malformado — " + e.getMessage(), e);
        }

        if (!(parsed instanceof List<?> list)) {
            throw new DataValidationException(jsonPath.toString(),
                "esperava um array JSON com 5 seções [p, pproc, pinc, ppre, pstat], "
                    + "mas o topo do arquivo é um " + parsed.getClass().getSimpleName());
        }
        if (list.size() != 5) {
            throw new DataValidationException(jsonPath.toString(),
                "esperava exatamente 5 seções [p, pproc, pinc, ppre, pstat], encontrou " + list.size());
        }

        ParamMap acquisition = toParamMap(jsonPath, list.get(0), "p (acquisition)");
        ParamMap processing = toParamMap(jsonPath, list.get(1), "pproc (processing)");
        ParamMap increment = toParamMap(jsonPath, list.get(2), "pinc (increment)");
        ParamMap preAcquisition = toParamMap(jsonPath, list.get(3), "ppre (pre-acquisition)");
        ParamMap status = toParamMap(jsonPath, list.get(4), "pstat (status)");

        return new ExperimentParameters(acquisition, processing, increment, preAcquisition, status);
    }

    @SuppressWarnings("unchecked")
    private static ParamMap toParamMap(Path jsonPath, Object section, String sectionName) {
        if (!(section instanceof Map<?, ?> map)) {
            throw new DataValidationException(jsonPath.toString(),
                "a seção '" + sectionName + "' deveria ser um objeto JSON, veio: "
                    + (section == null ? "null" : section.getClass().getSimpleName()));
        }
        return new ParamMap((Map<String, Object>) map);
    }
}
