package io.github.jongminchung.algorithm.study;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArrayDeque의 다양한 컬렉션 인터페이스 사용법을 테스트하는 클래스입니다.
 *
 * <h2>Stack 연산</h2>
 * <ul>
 *   <li>{@code push} - 요소를 스택의 맨 위에 추가</li>
 *   <li>{@code pop} - 스택의 맨 위 요소를 제거하고 반환</li>
 *   <li>{@code peek} - 스택의 맨 위 요소를 반환 (제거하지 않음)</li>
 * </ul>
 *
 * <h2>Queue 연산</h2>
 * <ul>
 *   <li>{@code offer} - 큐의 끝에 요소 추가</li>
 *   <li>{@code poll} - 큐의 맨 앞 요소를 제거하고 반환</li>
 *   <li>{@code peek} - 큐의 맨 앞 요소를 반환 (제거하지 않음)</li>
 * </ul>
 *
 * <p>Java Queue는 bounded queue(용량 제한 큐)와 unbounded queue(무제한 큐) 모두를 추상화합니다.</p>
 * <ul>
 *   <li>예외 기반 연산: add, remove, element</li>
 *   <li>상태 기반 연산: offer, poll, peek</li>
 * </ul>
 *
 * <h2>Deque 연산</h2>
 * <ul>
 *   <li>{@code offerFirst} - 덱의 앞에 요소 추가</li>
 *   <li>{@code offerLast} - 덱의 끝에 요소 추가</li>
 *   <li>{@code pollFirst} - 덱의 앞 요소를 제거하고 반환</li>
 *   <li>{@code pollLast} - 덱의 끝 요소를 제거하고 반환</li>
 *   <li>{@code peekFirst} - 덱의 앞 요소를 반환 (제거하지 않음)</li>
 *   <li>{@code peekLast} - 덱의 끝 요소를 반환 (제거하지 않음)</li>
 * </ul>
 */
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
        assertThat(deque.peek()).isNull();
    }
}
