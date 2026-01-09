package io.github.jongminchung.algorithm.codility.odd_occurrences_in_array;

import java.util.HashMap;

/**
 * <a href="https://app.codility.com/programmers/lessons/2-arrays/odd_occurrences_in_array">OddOccurrencesInArray</a>
 */
class Solution {

    int solution(int[] A) {
        var map = new HashMap<Integer, Integer>();

        for (var n : A) {
            map.put(n, map.getOrDefault(n, 0) + 1);
        }

        for (var e : map.entrySet()) {
            if (e.getValue() % 2 != 0) {
                return e.getKey();
            }
        }

        return 0;
    }
}
