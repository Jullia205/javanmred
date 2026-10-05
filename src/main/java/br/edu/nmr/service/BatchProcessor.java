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
        // Inicializamos as ferramentas de processamento com os parâmetros do experimento
        this.filter = new ButterworthFilter(exp.getParameters());
        this.fft = new FourierTransform();
        this.integrator = new PeakIntegrator();
    }

    /**
     * Processa todos os scans de um experimento em lote e devolve as séries (X, Y).
     *
     * @param exp O experimento carregado pelo módulo IO
     * @param startX O valor inicial da variável dependente (tempo de polarização, gradiente, etc.)
     * @param stepX O incremento aplicado a cada scan sucessivo
     * @return O objeto IntegratedSeries pronto para ser enviado para a função de Fit
     */
    public IntegratedSeries processAll(Experiment exp, double startX, double stepX) {
        List<TimeSignal> scans = exp.getScans();
        int numScans = scans.size();

        double[] xValues = new double[numScans];
        double[] yValues = new double[numScans];

        for (int i = 0; i < numScans; i++) {
            // 1. Construir o valor de X para o scan atual
            xValues[i] = startX + (i * stepX);

            // 2. Extrair o sinal no tempo bruto
            TimeSignal rawSignal = scans.get(i);

            // 3. Etapa 8: Filtro Digital e Janelamento
            TimeSignal filteredSignal = filter.apply(rawSignal);
            TimeSignal windowedSignal = Windowing.extractEcho(filteredSignal, exp.getParameters());

            // 4. Etapa 9: FFT
            Spectrum spectrum = fft.transform(windowedSignal);

            // 5. Etapa 10: Integração do Pico (Eixo Y)
            yValues[i] = integrator.integrate(spectrum, exp.getParameters());
        }

        return new IntegratedSeries(xValues, yValues);
    }
}