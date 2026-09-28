package br.edu.nmr.signal;

import br.edu.nmr.model.ExperimentParameters;
import br.edu.nmr.model.TimeSignal;

public class ButterworthFilter implements DigitalFilter {

    private final ExperimentParameters params;

    public ButterworthFilter(ExperimentParameters params) {
        this.params = params;
    }

    @Override
    public TimeSignal apply(TimeSignal inputSignal) {
        //implementar a lógica do filtro passa-faixa aqui.
        // Utilizar os limites digfmin e digfmax de this.params
        return inputSignal; // Retorno temporário
    }
}