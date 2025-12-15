package io.github.jongminchung.odata.query;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class FunctionCall implements FilterExpr {
    public enum Name {
        STARTSWITH,
        ENDSWITH,
        CONTAINS
    }

    private final Name name;
    private final List<FilterExpr> args;

    public FunctionCall(Name name, List<FilterExpr> args) {
        this.name = Objects.requireNonNull(name, "name");
        this.args = List.copyOf(args);
        if (this.args.size() != 2) throw new IllegalArgumentException("Function requires 2 args");
    }

    public static FunctionCall startswith(FilterExpr a, FilterExpr b) {
        return new FunctionCall(Name.STARTSWITH, Arrays.asList(a, b));
    }

    public static FunctionCall endswith(FilterExpr a, FilterExpr b) {
        return new FunctionCall(Name.ENDSWITH, Arrays.asList(a, b));
    }

    public static FunctionCall contains(FilterExpr a, FilterExpr b) {
        return new FunctionCall(Name.CONTAINS, Arrays.asList(a, b));
    }

    public Name name() {
        return name;
    }

    public List<FilterExpr> args() {
        return args;
    }
}
