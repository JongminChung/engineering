package io.github.jongminchung.algorithm.study;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringTest {

    @Test
    @DisplayName("String Builder")
    void method() {
        var sb = new StringBuilder("Hello");

        sb.append(" World");
        assertThat(sb.toString()).hasToString("Hello World");

        sb.reverse();
        assertThat(sb.toString()).hasToString("dlroW olleH");

        sb.reverse();
        sb.deleteCharAt(sb.length() - 1);
        assertThat(sb.toString()).hasToString("Hello Worl");

        sb.insert(sb.length(), "d");
        assertThat(sb.toString()).hasToString("Hello World");
    }
}
