package br.edu.nmr.analysis;

/** Modelo parametrico y = f(x; params) com gradiente analitico. */
public interface ModelFunction {
    int numParameters();
    double value(double x, double[] p);
    /** Derivadas parciais df/dp_j em x. */
    double[] gradient(double x, double[] p);
}
