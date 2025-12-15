package io.github.jongminchung.odata.query;

import java.util.Objects;

public final class Unary implements FilterExpr {
    private final UnaryOp op;
    private final FilterExpr expr;

    public Unary(UnaryOp op, FilterExpr expr) {
        this.op = Objects.requireNonNull(op, "op");
        this.expr = Objects.requireNonNull(expr, "expr");
    }

    public UnaryOp op() {
        return op;
    }

    public FilterExpr expr() {
        return expr;
    }
}
