package io.github.jongminchung.algorithm.study;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class PriorityQueueTest {

    @Test
    void test1() {
        // 기본: 오름차순
        var pq = new PriorityQueue<Integer>();

        var maxPq = new PriorityQueue<Integer>(Collections.reverseOrder());

        pq.offer(5);
        maxPq.offer(5);

        pq.offer(1);
        maxPq.offer(1);

        pq.offer(3);
        maxPq.offer(3);

        assertThat(pq.poll()).isEqualTo(1);
        assertThat(maxPq.poll()).isEqualTo(5);

        assertThat(pq.poll()).isEqualTo(3);
        assertThat(maxPq.poll()).isEqualTo(3);

        assertThat(pq.poll()).isEqualTo(5);
        assertThat(maxPq.poll()).isEqualTo(1);

        assertThat(pq.poll()).isNull();
    }

    static class Node implements Comparable<Node> {
        int index;
        int cost;

        public Node(int cost, int index) {
            this.cost = cost;
            this.index = index;
        }

        // 비교 로직: 비용(cost) 기준 오름차순
        @Override
        public int compareTo(@NonNull Node o) {
            return cost - o.cost;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return index == node.index && cost == node.cost;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, cost);
        }
    }

    @Test
    void nodeTest() {
        var pq = new PriorityQueue<Node>(Comparator.comparingInt(node -> node.cost));
        pq.offer(new Node(1, 1));
        pq.offer(new Node(5, 2));
        pq.offer(new Node(3, 3));

        assertThat(pq.poll()).isEqualTo(new Node(1, 1));
        assertThat(pq.poll()).isEqualTo(new Node(3, 3));
        assertThat(pq.poll()).isEqualTo(new Node(5, 2));
    }
}
