package br.edu.nmr.service;

import br.edu.nmr.io.ExperimentReader;
import br.edu.nmr.model.Experiment;
import br.edu.nmr.model.TimeSignal;
import br.edu.nmr.signal.DigitalFilter;
import br.edu.nmr.signal.ButterworthFilter;
import br.edu.nmr.signal.Windowing;
import br.edu.nmr.signal.FourierTransform;
import br.edu.nmr.signal.Spectrum;
import br.edu.nmr.analysis.PeakIntegrator;

import java.io.File;

public class AnalysisPipeline {

    private final ExperimentReader reader;

    public AnalysisPipeline() {
        this.reader = new ExperimentReader();
    }

    public double processSingleScan(File dataFile) throws Exception {

        // 1. Leitura (Etapa 7 - Módulo io existente)
        Experiment exp = reader.load(dataFile.toPath());

        TimeSignal rawSignal = exp.getScans().get(0);

        // 2. Processamento no Tempo (Etapa 8 - Filtro e Janela)
        DigitalFilter filter = new ButterworthFilter(exp.getParameters());
        TimeSignal filteredSignal = filter.apply(rawSignal);
        TimeSignal windowedSignal = Windowing.extractEcho(filteredSignal, exp.getParameters());

        // 3. Transformada de Fourier (Etapa 9)
        FourierTransform fft = new FourierTransform();
        Spectrum spectrum = fft.transform(windowedSignal);

        // 4. Integração do Pico (Etapa 10)
        PeakIntegrator integrator = new PeakIntegrator();
        double integralArea = integrator.integrate(spectrum, exp.getParameters());

        return integralArea;
    }
}