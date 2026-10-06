package br.edu.nmr.service;

import br.edu.nmr.analysis.PeakIntegrator;
import br.edu.nmr.model.Experiment;
import br.edu.nmr.model.IntegratedSeries;
import br.edu.nmr.model.TimeSignal;
import br.edu.nmr.signal.ButterworthFilter;
import br.edu.nmr.signal.DigitalFilter;
import br.edu.nmr.signal.FourierTransform;
import br.edu.nmr.signal.Spectrum;
import br.edu.nmr.signal.Windowing;

import java.util.List;

public class BatchProcessor {

    private final DigitalFilter filter;
    private final FourierTransform fft;
    private final PeakIntegrator integrator;

    public BatchProcessor(Experiment exp) {
        this.filter = new ButterworthFilter(exp.getParameters());
        this.fft = new FourierTransform();
        this.integrator = new PeakIntegrator();
    }

    public IntegratedSeries processAll(Experiment exp, double startX, double stepX) {
        List<TimeSignal> scans = exp.getScans();
        int numScans = scans.size();

        double[] xValues = new double[numScans];
        double[] yValues = new double[numScans];

        for (int i = 0; i < numScans; i++) {
            xValues[i] = startX + (i * stepX);
            TimeSignal rawSignal = scans.get(i);
            TimeSignal filteredSignal = filter.apply(rawSignal);
            TimeSignal windowedSignal = Windowing.extractEcho(filteredSignal, exp.getParameters());
            Spectrum spectrum = fft.transform(windowedSignal);
            yValues[i] = integrator.integrate(spectrum, exp.getParameters());
        }

        return new IntegratedSeries(xValues, yValues);
    }
}