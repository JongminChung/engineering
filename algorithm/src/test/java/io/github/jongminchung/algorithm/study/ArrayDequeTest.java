package io.github.jongminchung.algorithm.study;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

/// First, Last는 넣은 순서를 의미한다.
class ArrayDequeTest {

    @Test
    @DisplayName("add/offer/peek/remove 계열 메서드 정리")
    void queueAndDequeOperations() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();

        deque.addFirst(2);
        deque.addLast(3);
        deque.offerFirst(1);
        deque.offerLast(4);

        assertThat(deque).containsExactly(1, 2, 3, 4);
        assertThat(deque.getFirst()).isEqualTo(1);
        assertThat(deque.getLast()).isEqualTo(4);
        assertThat(deque.peekFirst()).isEqualTo(1);
        assertThat(deque.peekLast()).isEqualTo(4);

        assertThat(deque.removeFirst()).isEqualTo(1);
        assertThat(deque.removeLast()).isEqualTo(4);
        assertThat(deque.pollFirst()).isEqualTo(2);
        assertThat(deque.pollLast()).isEqualTo(3);
        assertThat(deque).isEmpty();
    }

    @Test
    @DisplayName("Stack 처럼 사용할 때는 push, pop")
    void stackStyleOperations() {
        ArrayDeque<String> deque = new ArrayDeque<>();

        deque.push("first");
        deque.push("second");
        deque.push("third");

        assertThat(deque).containsExactly("third", "second", "first");
        assertThat(deque.peek()).isEqualTo("third");
        assertThat(deque.pop()).isEqualTo("third");
        assertThat(deque.pop()).isEqualTo("second");
        assertThat(deque.pop()).isEqualTo("first");
        assertThat(deque).isEmpty();
    }
}
