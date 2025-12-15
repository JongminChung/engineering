package io.github.jongminchung.odata.query;

import java.util.Objects;

public final class Binary implements FilterExpr {
    private final FilterExpr left;
    private final BinaryOp op;
    private final FilterExpr right;

    public Binary(FilterExpr left, BinaryOp op, FilterExpr right) {
        this.left = Objects.requireNonNull(left, "left");
        this.op = Objects.requireNonNull(op, "op");
        this.right = Objects.requireNonNull(right, "right");
    }

    public FilterExpr left() {
        return left;
    }

    public BinaryOp op() {
        return op;
    }

    public FilterExpr right() {
        return right;
    }
}
