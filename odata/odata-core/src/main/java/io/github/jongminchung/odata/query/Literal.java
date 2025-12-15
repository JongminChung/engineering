package io.github.jongminchung.odata.query;

import java.time.OffsetDateTime;

public final class Literal implements FilterExpr {
    public enum Kind {
        STRING,
        NUMBER,
        BOOLEAN,
        NULL,
        DATETIME
    }

    private final Kind kind;
    private final Object value;

    private Literal(Kind kind, Object value) {
        this.kind = kind;
        this.value = value;
    }

    public static Literal of(Object value) {
        if (value == null) return new Literal(Kind.NULL, null);
        if (value instanceof String) return new Literal(Kind.STRING, value);
        if (value instanceof Boolean) return new Literal(Kind.BOOLEAN, value);
        if (value instanceof Number) return new Literal(Kind.NUMBER, value);
        if (value instanceof OffsetDateTime) return new Literal(Kind.DATETIME, value);
        // fallback to string
        return new Literal(Kind.STRING, String.valueOf(value));
    }

    public Kind kind() {
        return kind;
    }

    public Object value() {
        return value;
    }
}
