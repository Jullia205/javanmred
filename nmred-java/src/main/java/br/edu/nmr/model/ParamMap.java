package br.edu.nmr.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envelope tipado em cima de um Map&lt;String,Object&gt; vindo do JSON.
 *
 * <p>No Python original, cada seção de parâmetros (p, pproc, pinc, ppre,
 * pstat) é um dict comum e o código usa muito o idioma
 * {@code dict.get('chave', valor_padrao)}. Esta classe existe só para
 * reproduzir esse mesmo idioma em Java de forma segura (sem instanceof
 * espalhado pelo código todo,</p>
 *
 * <p>Os parâmetros variam de pulso a pulso (cpmg, diff, etc. têm campos
 * diferentes), então propositalmente NÃO viraram um POJO fortemente
 * tipado com um campo por parâmetro — isso quebraria a cada novo
 * programa de pulso. Quem precisar de um parâmetro específico pede por
 * nome, com um valor padrão explícito, igual ao Python.</p>
 */
public final class ParamMap {

    private final Map<String, Object> values;

    public ParamMap(Map<String, Object> values) {
        this.values = values == null ? new LinkedHashMap<>() : values;
    }

    public boolean has(String key) {
        return values.containsKey(key) && values.get(key) != null;
    }

    public Map<String, Object> raw() {
        return Collections.unmodifiableMap(values);
    }

    // ---- getters obrigatórios (lançam exceção se ausente) -----------------

    public double requireDouble(String key) {
        requireKey(key);
        return toDouble(values.get(key), key);
    }

    public int requireInt(String key) {
        requireKey(key);
        return toInt(values.get(key), key);
    }

    public String requireString(String key) {
        requireKey(key);
        return String.valueOf(values.get(key));
    }

    // ---- getters com valor padrão (idioma dict.get(key, default)) --------

    public double getDouble(String key, double defaultValue) {
        if (!has(key)) return defaultValue;
        return toDouble(values.get(key), key);
    }

    public int getInt(String key, int defaultValue) {
        if (!has(key)) return defaultValue;
        return toInt(values.get(key), key);
    }

    public String getString(String key, String defaultValue) {
        if (!has(key)) return defaultValue;
        return String.valueOf(values.get(key));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (!has(key)) return defaultValue;
        Object v = values.get(key);
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    // ---- helpers internos ---------------------------------------------

    private void requireKey(String key) {
        if (!has(key)) {
            throw new IllegalArgumentException(
                "Parâmetro obrigatório ausente no JSON: '" + key + "'");
        }
    }

    private static double toDouble(Object v, String key) {
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Parâmetro '" + key + "' deveria ser numérico, veio: " + v, e);
        }
    }

    private static int toInt(Object v, String key) {
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Parâmetro '" + key + "' deveria ser inteiro, veio: " + v, e);
        }
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
