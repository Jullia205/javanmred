package br.edu.nmr.model;

import java.sql.Time;
import java.util.Collections;
import java.util.List;

/**
 * Um experimento carregado de um par de arquivos (.json + .bin) —
 * equivalente ao objeto {@code expbase} do Python depois de {@code load()}.
 *
 * Contém os parâmetros do experimento e a lista de "scans" (repetições),
 * cada um representado por um {@link TimeSignal}. Em Python isso é
 * {@code self.tdt}, uma lista de objetos {@code timedata}.
 */
public final class Experiment {

    private final ExperimentParameters parameters;
    private final List<TimeSignal> scans;
    private final String sourceName;

    public Experiment(ExperimentParameters parameters, List<TimeSignal> scans, String sourceName) {
        if (scans == null || scans.isEmpty()) {
            throw new IllegalArgumentException("Um Experiment precisa de ao menos 1 scan");
        }
        this.parameters = parameters;
        this.scans = List.copyOf(scans);
        this.sourceName = sourceName;
    }

    public ExperimentParameters parameters() {
        return parameters;
    }

    public List<TimeSignal> scans() {
        return Collections.unmodifiableList(scans);
    }

    public int numScans() {
        return scans.size();
    }

    public TimeSignal scan(int index) {
        return scans.get(index);
    }

    /** Nome/base do arquivo de origem — útil em mensagens de log e de erro. */
    public String sourceName() {
        return sourceName;
    }

    public ExperimentParameters getParameters() {
        return this.parameters;
    }

    public List<TimeSignal> getScans() {
        return this.scans;
    }

    public String getName() {
    }
}
