package com.jsonlib;

public class JsonParser {

    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static JsonValue parse(String json) {
        if (json == null || json.isBlank()) {
            throw new JsonException("Input is null or empty");
        }
        JsonParser parser = new JsonParser(json.trim());
        JsonValue value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != parser.src.length()) {
            throw new JsonException("Unexpected trailing characters at position " + parser.pos);
        }
        return value;
    }

    public static JsonObject parseObject(String json) {
        JsonValue v = parse(json);
        if (!v.isObject()) throw new JsonException("Expected JSON object but got: " + v.getClass().getSimpleName());
        return v.asObject();
    }

    public static JsonArray parseArray(String json) {
        JsonValue v = parse(json);
        if (!v.isArray()) throw new JsonException("Expected JSON array but got: " + v.getClass().getSimpleName());
        return v.asArray();
    }

    private JsonValue parseValue() {
        skipWhitespace();
        if (pos >= src.length()) throw new JsonException("Unexpected end of input");

        char c = src.charAt(pos);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't') return parseTrue();
        if (c == 'f') return parseFalse();
        if (c == 'n') return parseNull();
        if (c == '-' || Character.isDigit(c)) return parseNumber();

        throw new JsonException("Unexpected character '" + c + "' at position " + pos);
    }

    private JsonObject parseObject() {
        consume('{');
        JsonObject obj = new JsonObject();
        skipWhitespace();

        if (peek() == '}') { pos++; return obj; }

        while (true) {
            skipWhitespace();
            String key = parseStringValue();
            skipWhitespace();
            consume(':');
            JsonValue value = parseValue();
            obj.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == '}') { pos++; break; }
            if (next == ',') { pos++; continue; }
            throw new JsonException("Expected ',' or '}' at position " + pos);
        }
        return obj;
    }

    private JsonArray parseArray() {
        consume('[');
        JsonArray arr = new JsonArray();
        skipWhitespace();

        if (peek() == ']') { pos++; return arr; }

        while (true) {
            arr.add(parseValue());
            skipWhitespace();
            char next = peek();
            if (next == ']') { pos++; break; }
            if (next == ',') { pos++; continue; }
            throw new JsonException("Expected ',' or ']' at position " + pos);
        }
        return arr;
    }

    private JsonString parseString() {
        return new JsonString(parseStringValue());
    }

    private String parseStringValue() {
        consume('"');
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos >= src.length()) break;
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > src.length()) {
                            throw new JsonException("Invalid unicode escape at position " + pos);
                        }
                        String hex = src.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                        break;
                    default:
                        throw new JsonException("Invalid escape character '\\" + esc + "' at position " + pos);
                }
            } else {
                if (c < 0x20) {
                    throw new JsonException("Unescaped control character at position " + (pos - 1));
                }
                sb.append(c);
            }
        }
        throw new JsonException("Unterminated string at position " + pos);
    }

    private JsonNumber parseNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
            throw new JsonException("Invalid number at position " + pos);
        }
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        boolean isFloat = false;
        if (pos < src.length() && src.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                throw new JsonException("Invalid number at position " + pos);
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) pos++;
            if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                throw new JsonException("Invalid exponent at position " + pos);
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        String raw = src.substring(start, pos);
        try {
            if (isFloat) return new JsonNumber(Double.parseDouble(raw));
            try {
                return new JsonNumber(Long.parseLong(raw));
            } catch (NumberFormatException e) {
                return new JsonNumber(Double.parseDouble(raw));
            }
        } catch (NumberFormatException e) {
            throw new JsonException("Invalid number: " + raw);
        }
    }

    private JsonBoolean parseTrue() {
        expectLiteral("true");
        return JsonBoolean.TRUE;
    }

    private JsonBoolean parseFalse() {
        expectLiteral("false");
        return JsonBoolean.FALSE;
    }

    private JsonNull parseNull() {
        expectLiteral("null");
        return JsonNull.INSTANCE;
    }

    private void expectLiteral(String literal) {
        if (!src.startsWith(literal, pos)) {
            throw new JsonException("Expected '" + literal + "' at position " + pos);
        }
        pos += literal.length();
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        if (pos >= src.length()) throw new JsonException("Unexpected end of input");
        return src.charAt(pos);
    }

    private void consume(char expected) {
        if (pos >= src.length() || src.charAt(pos) != expected) {
            throw new JsonException("Expected '" + expected + "' at position " + pos
                    + (pos < src.length() ? " but got '" + src.charAt(pos) + "'" : " but reached end of input"));
        }
        pos++;
    }
}
