package io.github.jongminchung.odata.query;

public final class Property implements FilterExpr {
    private final String name;

    public Property(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("property name");
        this.name = name;
    }

    public String name() {
        return name;
    }
}
