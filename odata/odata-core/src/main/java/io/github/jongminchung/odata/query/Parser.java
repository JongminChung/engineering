package io.github.jongminchung.odata.query;

/**
 * Recursive-descent parser for a subset of OData $filter grammar.
 */
final class Parser {
    private final Tokenizer tz;
    private Token lookahead;

    Parser(Tokenizer tz) {
        this.tz = tz;
        this.lookahead = tz.next();
    }

    FilterExpr parseExpression() {
        FilterExpr expr = parseOr();
        if (lookahead.type != TokenType.EOF && lookahead.type != TokenType.RPAREN && lookahead.type != TokenType.COMMA) {
            throw error("Unexpected token '" + lookahead.text + "'");
        }
        return expr;
    }

    private FilterExpr parseOr() {
        FilterExpr left = parseAnd();
        while (lookahead.type == TokenType.OR) {
            consume(TokenType.OR);
            FilterExpr right = parseAnd();
            left = new Binary(left, BinaryOp.OR, right);
        }
        return left;
    }

    private FilterExpr parseAnd() {
        FilterExpr left = parseNot();
        while (lookahead.type == TokenType.AND) {
            consume(TokenType.AND);
            FilterExpr right = parseNot();
            left = new Binary(left, BinaryOp.AND, right);
        }
        return left;
    }

    private FilterExpr parseNot() {
        if (lookahead.type == TokenType.NOT) {
            consume(TokenType.NOT);
            return new Unary(UnaryOp.NOT, parsePrimary());
        }
        return parsePrimary();
    }

    private FilterExpr parsePrimary() {
        switch (lookahead.type) {
            case LPAREN:
                consume(TokenType.LPAREN);
                FilterExpr e = parseOr();
                consume(TokenType.RPAREN);
                return e;
            case IDENT:
                return parseAfterIdent();
            case STRING:
            case NUMBER:
            case BOOLEAN:
            case NULL:
                return parseLiteral();
            default:
                throw error("Unexpected token '" + lookahead.text + "'");
        }
    }

    private FilterExpr parseAfterIdent() {
        String ident = lookahead.text;
        consume(TokenType.IDENT);
        // function call?
        if (lookahead.type == TokenType.LPAREN) {
            consume(TokenType.LPAREN);
            FilterExpr arg1 = parseOr();
            consume(TokenType.COMMA);
            FilterExpr arg2 = parseOr();
            consume(TokenType.RPAREN);
            String fn = ident.toLowerCase();
            switch (fn) {
                case "startswith": return FunctionCall.startswith(arg1, arg2);
                case "endswith": return FunctionCall.endswith(arg1, arg2);
                case "contains": return FunctionCall.contains(arg1, arg2);
                default: throw error("Unknown function '" + ident + "'");
            }
        }
        // otherwise it's a property; maybe binary comparison follows
        Property prop = new Property(ident);
        switch (lookahead.type) {
            case OP_EQ: consume(TokenType.OP_EQ); return new Binary(prop, BinaryOp.EQ, parsePrimary());
            case OP_NE: consume(TokenType.OP_NE); return new Binary(prop, BinaryOp.NE, parsePrimary());
            case OP_GT: consume(TokenType.OP_GT); return new Binary(prop, BinaryOp.GT, parsePrimary());
            case OP_GE: consume(TokenType.OP_GE); return new Binary(prop, BinaryOp.GE, parsePrimary());
            case OP_LT: consume(TokenType.OP_LT); return new Binary(prop, BinaryOp.LT, parsePrimary());
            case OP_LE: consume(TokenType.OP_LE); return new Binary(prop, BinaryOp.LE, parsePrimary());
            default:
                // single identifier not allowed as expression in our subset
                throw error("Missing operator after property '" + ident + "'");
        }
    }

    private Literal parseLiteral() {
        switch (lookahead.type) {
            case STRING: {
                String v = lookahead.text; consume(TokenType.STRING); return Literal.of(v);
            }
            case NUMBER: {
                String n = lookahead.text; consume(TokenType.NUMBER);
                if (n.contains(".")) return Literal.of(Double.parseDouble(n));
                try { return Literal.of(Long.parseLong(n)); } catch (NumberFormatException ex) { return Literal.of(Double.parseDouble(n)); }
            }
            case BOOLEAN: {
                boolean b = Boolean.parseBoolean(lookahead.text); consume(TokenType.BOOLEAN); return Literal.of(b);
            }
            case NULL: {
                consume(TokenType.NULL); return Literal.of(null);
            }
            default:
                throw error("Expected literal");
        }
    }

    private void consume(TokenType type) {
        if (lookahead.type != type) throw error("Expected '" + type + "' but was '" + lookahead.type + "'");
        lookahead = tz.next();
    }

    private ODataParseException error(String msg) {
        return new ODataParseException(msg, lookahead.pos);
    }
}
