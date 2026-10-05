package br.edu.nmr.analysis;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

public class RelaxationFit {

    /**
     * Ajusta os pontos (X, Y) ao modelo exponencial de relaxação (Etapa 11).
     *
     * @param times Eixo X (tempos de polarização ou tempos de eco em segundos)
     * @param integrals Eixo Y (áreas calculadas pelo PeakIntegrator)
     * @param guessA Valor inicial estimado para a amplitude (A)
     * @param guessR Valor inicial estimado para a taxa de relaxação (R)
     * @param guessC Valor inicial estimado para o ruído de fundo (C)
     * @return Array com os parâmetros otimizados: [A, R, C]
     */
    public double[] fit(double[] times, double[] integrals, double guessA, double guessR, double guessC) {

        // 1. Carregar os pontos experimentais no formato exigido pela biblioteca
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = 0; i < times.length; i++) {
            obs.add(times[i], integrals[i]);
        }

        // 2. Definir a função física y(t) = A * exp(-R * t) + C e o seu gradiente
        ParametricUnivariateFunction function = new ParametricUnivariateFunction() {

            @Override
            public double value(double t, double... parameters) {
                double a = parameters[0];
                double r = parameters[1];
                double c = parameters[2];
                return a * Math.exp(-r * t) + c;
            }

            @Override
            public double[] gradient(double t, double... parameters) {
                double a = parameters[0];
                double r = parameters[1];

                double expTerm = Math.exp(-r * t);

                // Derivadas parciais relativas a [A, R, C]
                return new double[] {
                        expTerm,              // Derivada dY/dA
                        -a * t * expTerm,     // Derivada dY/dR
                        1.0                   // Derivada dY/dC
                };
            }
        };

        // 3. Inicializar o ajustador com a função e as estimativas iniciais
        SimpleCurveFitter fitter = SimpleCurveFitter.create(function, new double[]{guessA, guessR, guessC});

        // 4. Executar a otimização e devolver os parâmetros finais
        return fitter.fit(obs.toList());
    }
}