package io.github.jongminchung.odata.query;

final class Tokenizer {
    private final String s;
    private int i = 0;

    Tokenizer(String s) { this.s = s; }

    Token next() {
        skipWs();
        if (i >= s.length()) return new Token(TokenType.EOF, "", i);
        char c = s.charAt(i);
        int start = i;
        // punctuation
        if (c == '(') { i++; return new Token(TokenType.LPAREN, "(", start); }
        if (c == ')') { i++; return new Token(TokenType.RPAREN, ")", start); }
        if (c == ',') { i++; return new Token(TokenType.COMMA, ",", start); }
        // operators by text (ge, le, ne)
        if (isAlpha(c)) {
            String ident = readIdent();
            String low = ident.toLowerCase();
            switch (low) {
                case "and": return new Token(TokenType.AND, ident, start);
                case "or": return new Token(TokenType.OR, ident, start);
                case "not": return new Token(TokenType.NOT, ident, start);
                case "eq": return new Token(TokenType.OP_EQ, ident, start);
                case "ne": return new Token(TokenType.OP_NE, ident, start);
                case "gt": return new Token(TokenType.OP_GT, ident, start);
                case "ge": return new Token(TokenType.OP_GE, ident, start);
                case "lt": return new Token(TokenType.OP_LT, ident, start);
                case "le": return new Token(TokenType.OP_LE, ident, start);
                case "true": case "false": return new Token(TokenType.BOOLEAN, low, start);
                case "null": return new Token(TokenType.NULL, low, start);
                default: return new Token(TokenType.IDENT, ident, start);
            }
        }
        if (c == '\'') { return readString(); }
        if (isDigit(c) || (c == '-' && i+1 < s.length() && isDigit(s.charAt(i+1)))) {
            return readNumber();
        }
        // unknown
        throw new ODataParseException("Unexpected character '" + c + "'", i);
    }

    private void skipWs() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++; else break;
        }
    }

    private boolean isAlpha(char c) { return Character.isLetter(c) || c == '_' || c == '$'; }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    private String readIdent() {
        int start = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$' ) i++; else break;
        }
        return s.substring(start, i);
    }

    private Token readString() {
        int start = i; // at '
        i++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '\'') {
                if (i < s.length() && s.charAt(i) == '\'') { // escaped quote ''
                    sb.append('\'');
                    i++;
                } else {
                    // end of string
                    return new Token(TokenType.STRING, sb.toString(), start);
                }
            } else {
                sb.append(c);
            }
        }
        throw new ODataParseException("Unterminated string", start);
    }

    private Token readNumber() {
        int start = i;
        if (s.charAt(i) == '-') i++;
        while (i < s.length() && isDigit(s.charAt(i))) i++;
        if (i < s.length() && s.charAt(i) == '.') {
            i++; while (i < s.length() && isDigit(s.charAt(i))) i++;
        }
        return new Token(TokenType.NUMBER, s.substring(start, i), start);
    }
}
