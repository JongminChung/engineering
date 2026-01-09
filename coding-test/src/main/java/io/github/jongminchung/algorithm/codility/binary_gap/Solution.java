package io.github.jongminchung.algorithm.codility.binary_gap;

/** <a href="https://app.codility.com/programmers/lessons/1-iterations/binary_gap">Binary Gap</a> */
class Solution {

    int solution(int N) {
        var binary = Integer.toBinaryString(N);
        var maxGap = 0;
        var current = 0;
        var inGap = false;

        for (var c : binary.toCharArray()) {
            if (c == '1') {
                if (inGap) {
                    maxGap = Math.max(maxGap, current);
                    current = 0;
                }
                inGap = true;
            } else if (inGap) {
                current++;
            }
        }

        return maxGap;
    }
}
