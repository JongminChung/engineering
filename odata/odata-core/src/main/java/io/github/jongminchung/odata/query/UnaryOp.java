package io.github.jongminchung.odata.query;

public enum UnaryOp {
    NOT("not");

    private final String keyword;

    UnaryOp(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }
}
