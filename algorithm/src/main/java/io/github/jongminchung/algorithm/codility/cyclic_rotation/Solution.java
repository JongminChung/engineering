package io.github.jongminchung.algorithm.codility.cyclic_rotation;

/**
 * <a href="https://app.codility.com/programmers/lessons/2-arrays/cyclic_rotation/">CyclicRotation</a>
 */
class Solution {

    int[] solution(int[] A, int K) {
        var n = A.length;
        if (n == 0) return A;

        K = K % n;
        if (K == 0) return A; // 회전 필요 없음

        var result = new int[n];
        for (int i = 0; i < n; i++) {
            result[(i + K) % n] = A[i];
        }

        return result;
    }
}

