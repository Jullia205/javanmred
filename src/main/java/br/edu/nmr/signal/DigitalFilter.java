package br.edu.nmr.signal;

import br.edu.nmr.model.TimeSignal;

public interface DigitalFilter {
    TimeSignal apply(TimeSignal inputSignal);
}