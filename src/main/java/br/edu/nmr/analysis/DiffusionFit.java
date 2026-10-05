package br.edu.nmr.analysis;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

public class DiffusionFit {

    /**
     * Ajusta os pontos de atenuação do sinal ao modelo de difusão de Stejskal-Tanner (Etapa 12).
     *
     * @param smallDeltas Eixo X: Durações do pulso de gradiente (delta pequeno) em segundos
     * @param integrals Eixo Y: Áreas do pico integradas para cada gradiente
     * @param bigDelta Parâmetro de atraso fixo entre os gradientes (Delta grande), geralmente extraído de 'tau'
     * @param guessA Valor inicial estimado para a amplitude (A)
     * @param guessB Valor inicial estimado para o fator de difusão/gradiente (B)
     * @return Array com os parâmetros otimizados: [A, B]
     */
    public double[] fit(double[] smallDeltas, double[] integrals, double bigDelta, double guessA, double guessB) {

        // 1. Carregar os pontos experimentais no formato da biblioteca
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = 0; i < smallDeltas.length; i++) {
            obs.add(smallDeltas[i], integrals[i]);
        }

        // 2. Definir a função física y(x) = A * exp(-B * x^2 * (BigDelta - x/3)) e o seu gradiente
        // Nota: O 'x' aqui representa o pequeno delta (duração do gradiente)
        ParametricUnivariateFunction function = new ParametricUnivariateFunction() {

            @Override
            public double value(double x, double... parameters) {
                double a = parameters[0];
                double b = parameters[1];

                // Cálculo do termo exponencial: exp(-B * x^2 * (Delta - x/3))
                return a * Math.exp(-b * (x * x) * (bigDelta - x / 3.0));
            }

            @Override
            public double[] gradient(double x, double... parameters) {
                double a = parameters[0];
                double b = parameters[1];

                double term = (x * x) * (bigDelta - x / 3.0);
                double expTerm = Math.exp(-b * term);

                // Derivadas parciais relativas a [A, B]
                return new double[] {
                        expTerm,               // Derivada dY/dA
                        -a * term * expTerm    // Derivada dY/dB
                };
            }
        };

        // 3. Inicializar o ajustador com a função e as estimativas iniciais
        SimpleCurveFitter fitter = SimpleCurveFitter.create(function, new double[]{guessA, guessB});

        // 4. Executar a otimização e devolver os parâmetros
        return fitter.fit(obs.toList());
    }
}