package io.github.jongminchung.odata.query;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class Serializer {
    static String serialize(FilterExpr expr) {
        StringBuilder sb = new StringBuilder();
        write(expr, sb, 0);
        return sb.toString();
    }

    private static int precedence(FilterExpr e) {
        if (e instanceof Unary) return 3;
        if (e instanceof Binary) {
            var op = ((Binary) e).op();
            if (op == BinaryOp.AND) return 2;
            if (op == BinaryOp.OR) return 1;
            return 4; // comparisons highest among binaries to avoid parentheses usually
        }
        return 5; // literals, properties, functions
    }

    private static void write(FilterExpr e, StringBuilder sb, int parentPrec) {
        int prec = precedence(e);
        boolean needParens = prec < parentPrec;
        if (needParens) sb.append('(');
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            write(b.left(), sb, prec);
            sb.append(' ').append(b.op().keyword()).append(' ');
            write(b.right(), sb, prec + (b.op() == BinaryOp.AND || b.op() == BinaryOp.OR ? 1 : 0));
        } else if (e instanceof Unary) {
            Unary u = (Unary) e;
            sb.append(u.op().keyword()).append(' ');
            write(u.expr(), sb, prec);
        } else if (e instanceof Property) {
            sb.append(((Property) e).name());
        } else if (e instanceof Literal) {
            writeLiteral((Literal) e, sb);
        } else if (e instanceof FunctionCall) {
            FunctionCall f = (FunctionCall) e;
            String fn = switch (f.name()) {
                case STARTSWITH -> "startswith";
                case ENDSWITH -> "endswith";
                case CONTAINS -> "contains";
            };
            sb.append(fn).append('(');
            write(f.args().get(0), sb, 0);
            sb.append(',');
            write(f.args().get(1), sb, 0);
            sb.append(')');
        } else {
            throw new IllegalStateException("Unknown expr: " + e);
        }
        if (needParens) sb.append(')');
    }

    private static void writeLiteral(Literal lit, StringBuilder sb) {
        switch (lit.kind()) {
            case NULL:
                sb.append("null");
                return;
            case BOOLEAN:
                sb.append(((Boolean) lit.value()) ? "true" : "false");
                return;
            case NUMBER:
                sb.append(String.valueOf(lit.value()));
                return;
            case DATETIME:
                // v1 simple ISO-8601 string literal
                sb.append('\'').append(lit.value().toString().replace("'", "''")).append('\'');
                return;
            case STRING:
            default:
                sb.append('\'').append(String.valueOf(lit.value()).replace("'", "''")).append('\'');
        }
    }
}
