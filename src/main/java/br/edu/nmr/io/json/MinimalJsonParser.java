package br.edu.nmr.io.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser de JSON minimalista, sem dependências externas.
 *
 * <p>Os arquivos .json salvos pelo programa Python (expbase.save/saveparams)
 * são um array de 5 objetos: [p, pproc, pinc, ppre, pstat]. Cada objeto tem
 * campos numéricos, strings, arrays, booleanos e (raramente) null — nada
 * de estruturas exóticas. Isso é simples o bastante para não precisar de
 * uma biblioteca como Jackson só para isso.</p>
 *
 * <p>Mapeamento de tipos:</p>
 * <ul>
 *   <li>objeto JSON  -&gt; {@link LinkedHashMap}&lt;String, Object&gt; (preserva ordem)</li>
 *   <li>array JSON   -&gt; {@link List}&lt;Object&gt;</li>
 *   <li>número       -&gt; {@link Long} se não tiver ponto/expoente, {@link Double} caso contrário</li>
 *   <li>string       -&gt; {@link String}</li>
 *   <li>true/false   -&gt; {@link Boolean}</li>
 *   <li>null         -&gt; {@code null}</li>
 * </ul>
 *
 * Se seu grupo preferir usar o Jackson (com.fasterxml.jackson), esta classe
 * pode ser substituída por um ObjectMapper sem alterar o resto do projeto —
 * ela só é usada dentro de {@link br.edu.nmr.io.ParameterReader}.
 */
public final class MinimalJsonParser {

    private final String src;
    private int pos;

    private MinimalJsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    /** Faz o parse de um documento JSON completo (objeto, array, ou valor simples). */
    public static Object parse(String json) {
        MinimalJsonParser p = new MinimalJsonParser(json);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (p.pos != p.src.length()) {
            throw new JsonParseException("Texto sobrando após o fim do JSON, na posição " + p.pos);
        }
        return value;
    }

    private Object parseValue() {
        if (pos >= src.length()) {
            throw new JsonParseException("Fim inesperado do JSON na posição " + pos);
        }
        char c = src.charAt(pos);
        switch (c) {
            case '{': return parseObject();
            case '[': return parseArray();
            case '"': return parseString();
            case 't': return parseLiteral("true", Boolean.TRUE);
            case 'f': return parseLiteral("false", Boolean.FALSE);
            case 'n': return parseLiteral("null", null);
            default:
                if (c == '-' || Character.isDigit(c)) {
                    return parseNumber();
                }
                throw new JsonParseException("Caractere inesperado '" + c + "' na posição " + pos);
        }
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            } else if (c == '}') {
                break;
            } else {
                throw new JsonParseException("Esperava ',' ou '}' na posição " + (pos - 1));
            }
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            } else if (c == ']') {
                break;
            } else {
                throw new JsonParseException("Esperava ',' ou ']' na posição " + (pos - 1));
            }
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw new JsonParseException("String não terminada (fim do arquivo)");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        String hex = src.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                        break;
                    default:
                        throw new JsonParseException("Escape inválido '\\" + esc + "' na posição " + pos);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Object parseNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        boolean isDouble = false;
        if (pos < src.length() && src.charAt(pos) == '.') {
            isDouble = true;
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            isDouble = true;
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        String num = src.substring(start, pos);
        if (isDouble) {
            return Double.parseDouble(num);
        }
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            return Double.parseDouble(num);
        }
    }

    private Object parseLiteral(String literal, Object value) {
        if (pos + literal.length() > src.length() || !src.startsWith(literal, pos)) {
            throw new JsonParseException("Literal inválido na posição " + pos + " (esperava '" + literal + "')");
        }
        pos += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        if (pos >= src.length()) throw new JsonParseException("Fim inesperado do JSON");
        return src.charAt(pos);
    }

    private char next() {
        if (pos >= src.length()) throw new JsonParseException("Fim inesperado do JSON");
        return src.charAt(pos++);
    }

    private void expect(char c) {
        char got = next();
        if (got != c) {
            throw new JsonParseException("Esperava '" + c + "' mas encontrou '" + got + "' na posição " + (pos - 1));
        }
    }

    /** Erro de sintaxe do JSON de parâmetros — sempre indica a posição do problema. */
    public static final class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }
}
