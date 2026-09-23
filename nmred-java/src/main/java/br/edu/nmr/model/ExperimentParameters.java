package br.edu.nmr.model;

/**
 * As cinco seções de parâmetros salvas pelo programa Python
 * (expbase.saveparams: {@code json.dump((p, pproc, pinc, ppre, pstat), ...)}).
 *
 * A ORDEM importa: o JSON é um array posicional, não um objeto com nomes
 * de campo. A ordem de gravação no Python é sempre:
 * <pre>  [p, pproc, pinc, ppre, pstat]</pre>
 *
 * <ul>
 *   <li>{@code acquisition} (p)        — parâmetros do programa de pulso: p1, frq, nsamp, srate, necho, tau...</li>
 *   <li>{@code processing} (pproc)     — parâmetros de processamento: ftmin, ftmax, wdw, intmin, intmax...</li>
 *   <li>{@code increment} (pinc)       — parâmetros de repetição/varredura: nav, n, incp, inc, d...</li>
 *   <li>{@code preAcquisition} (ppre)  — parâmetros de pré-polarização: poltime, polamp...</li>
 *   <li>{@code status} (pstat)         — metadados gravados após a aquisição: ninchann, nsampx, sratex...</li>
 * </ul>
 */
public final class ExperimentParameters {

    private final ParamMap acquisition;
    private final ParamMap processing;
    private final ParamMap increment;
    private final ParamMap preAcquisition;
    private final ParamMap status;

    public ExperimentParameters(ParamMap acquisition, ParamMap processing, ParamMap increment,
                                 ParamMap preAcquisition, ParamMap status) {
        this.acquisition = acquisition;
        this.processing = processing;
        this.increment = increment;
        this.preAcquisition = preAcquisition;
        this.status = status;
    }

    public ParamMap acquisition() { return acquisition; }
    public ParamMap processing() { return processing; }
    public ParamMap increment() { return increment; }
    public ParamMap preAcquisition() { return preAcquisition; }
    public ParamMap status() { return status; }

    /**
     * Taxa de amostragem efetiva, replicando expbase.getsrate():
     * usa pstat['sratex'] se existir (taxa real medida durante a aquisição),
     * senão cai para p['srate'] (taxa nominal pedida).
     */
    public double effectiveSampleRate() {
        if (status.has("sratex")) {
            return status.requireDouble("sratex");
        }
        return acquisition.requireDouble("srate");
    }
}
