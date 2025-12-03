package io.github.jongminchung.algorithm.codility.binary_gap;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class SolutionTest {

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            N, EXPECTED
            32, 0
            529, 4
            1041, 5
            """)
    void test(int n, int expected) {
        int result = new Solution().solution(n);
        assertThat(result).isEqualTo(expected);
    }
}
