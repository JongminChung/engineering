package io.github.jongminchung.odata.query;

public enum BinaryOp {
    EQ("eq"),
    NE("ne"),
    GT("gt"),
    GE("ge"),
    LT("lt"),
    LE("le"),
    AND("and"),
    OR("or");

    private final String keyword;

    BinaryOp(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }
}
