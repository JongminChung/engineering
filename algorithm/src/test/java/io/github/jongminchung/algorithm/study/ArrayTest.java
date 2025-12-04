package io.github.jongminchung.algorithm.study;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class ArrayTest {

    @Test
    @DisplayName("Arrays.copyOfRange(arr, start, end): 부분 복사")
    void copyOfRange() {
        var array = new int[]{1, 2, 3, 4, 5};

        var result = Arrays.copyOfRange(array, 1, 3);

        assertThat(result).containsExactly(2, 3);
    }

    @Test
    @DisplayName("Arrays.sort(arr): 정렬")
    void sort() {
        var array = new int[]{5, 3, 1, 2, 4};

        Arrays.sort(array);

        assertThat(array).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("Arrays.sort(arrayOfObjectClass, comparator): 정렬을 직접 정의")
    void sortReversed() {
        var array = new int[]{5, 3, 1, 2, 4};

        var integerArray = new Integer[array.length];

        var idx = 0;
        for (int el : array) {
            integerArray[idx++] = el;
        }

        Arrays.sort(integerArray, (it1, it2) -> Integer.compare(it2, it1));
        assertThat(integerArray).containsExactly(5, 4, 3, 2, 1);
    }
}
