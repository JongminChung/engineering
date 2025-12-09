package io.github.jongminchung.odata.query;

import java.util.Objects;

/**
 * Generic typed Filter wrapper around a parsed {@link FilterExpr}.
 */
public final class Filter<T> {
    private final FilterExpr expr;

    private Filter(FilterExpr expr) {
        this.expr = Objects.requireNonNull(expr, "expr");
    }

    public static <T> Filter<T> of(FilterExpr expr) {
        return new Filter<>(expr);
    }

    public FilterExpr expr() {
        return expr;
    }

    public static <T> Filter<T> parse(String input) {
        if (input == null || input.isBlank()) return null;
        var parser = new Parser(new Tokenizer(input));
        return new Filter<>(parser.parseExpression());
    }

    // Builder helpers
    public static FilterExpr eq(String field, Object value) { return new Binary(new Property(field), BinaryOp.EQ, Literal.of(value)); }
    public static FilterExpr ne(String field, Object value) { return new Binary(new Property(field), BinaryOp.NE, Literal.of(value)); }
    public static FilterExpr gt(String field, Object value) { return new Binary(new Property(field), BinaryOp.GT, Literal.of(value)); }
    public static FilterExpr ge(String field, Object value) { return new Binary(new Property(field), BinaryOp.GE, Literal.of(value)); }
    public static FilterExpr lt(String field, Object value) { return new Binary(new Property(field), BinaryOp.LT, Literal.of(value)); }
    public static FilterExpr le(String field, Object value) { return new Binary(new Property(field), BinaryOp.LE, Literal.of(value)); }
    public static FilterExpr and(FilterExpr left, FilterExpr right) { return new Binary(left, BinaryOp.AND, right); }
    public static FilterExpr or(FilterExpr left, FilterExpr right) { return new Binary(left, BinaryOp.OR, right); }
    public static FilterExpr not(FilterExpr inner) { return new Unary(UnaryOp.NOT, inner); }
    public static FilterExpr startswith(String field, String value) { return FunctionCall.startswith(new Property(field), Literal.of(value)); }
    public static FilterExpr endswith(String field, String value) { return FunctionCall.endswith(new Property(field), Literal.of(value)); }
    public static FilterExpr contains(String field, String value) { return FunctionCall.contains(new Property(field), Literal.of(value)); }

    @Override
    public String toString() {
        return Serializer.serialize(expr);
    }
}
