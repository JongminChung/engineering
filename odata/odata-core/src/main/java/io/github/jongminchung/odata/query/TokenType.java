package io.github.jongminchung.odata.query;

enum TokenType {
    IDENT, STRING, NUMBER, BOOLEAN, NULL,
    LPAREN, RPAREN, COMMA,
    OP_EQ, OP_NE, OP_GT, OP_GE, OP_LT, OP_LE,
    AND, OR, NOT,
    EOF
}
