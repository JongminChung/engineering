package io.github.jongminchung.algorithm.codility.ncp;

class Solution {

    public int solution1(String[] E) {
        int n = E.length;
        int max = 0;

        // day1: 0 ~ 9
        for (int d1 = 0; d1 < 10; d1++) {
            char c1 = (char) ('0' + d1);

            // day2: d1 ~ 9  (같은 날 두 번 선택도 허용)
            for (int d2 = d1; d2 < 10; d2++) {
                char c2 = (char) ('0' + d2);
                int count = 0;

                // 각 직원의 가능한 요일 문자열 확인
                for (String s : E) {
                    if (s.indexOf(c1) >= 0 || s.indexOf(c2) >= 0) {
                        count++;
                    }
                }

                if (count > max) {
                    max = count;
                }
            }
        }

        return max;
    }

    int solution2(String s) {
        int n = s.length();
        // dp[c] : 마지막 문자가 (char)('a' + c) 인
        //         정렬된 부분수열의 최대 길이
        int[] dp = new int[26];

        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            int idx = ch - 'a';

            // ch 보다 작거나 같은 문자로 끝나는 부분수열 중 최대 길이 찾기
            int best = 0;
            for (int c = 0; c <= idx; c++) {
                if (dp[c] > best) {
                    best = dp[c];
                }
            }

            int newLen = best + 1;
            if (newLen > dp[idx]) {
                dp[idx] = newLen;
            }
        }

        // LNDS 길이 = dp 배열 중 최대값
        int lnds = 0;
        for (int len : dp) {
            if (len > lnds) {
                lnds = len;
            }
        }

        // 최소 삭제 개수 = 전체 길이 - 남길 수 있는 최대 길이
        return n - lnds;
    }

    public int solution3(int[] A, int[] B) {
        int n = A.length;
        final int MAX_VAL = 100000; // 문제에서 주어진 값의 최대

        // forced[v] == true 면 v는 (v, v)로 한 번 이상 등장한 "강제 숫자"
        boolean[] forced = new boolean[MAX_VAL + 2]; // 1..100001 사용

        for (int i = 0; i < n; i++) {
            if (A[i] == B[i]) {
                int v = A[i]; // == B[i]
                // 값 범위는 이미 1..100000 이지만, 방어적으로 한 번 더 체크해도 됨
                if (v >= 1 && v <= MAX_VAL + 1) {
                    forced[v] = true;
                }
            }
        }

        // 1부터 시작해서, 강제 숫자 집합에 없는 가장 작은 양의 정수를 찾는다.
        int answer = 1;
        while (answer <= MAX_VAL + 1 && forced[answer]) {
            answer++;
        }

        return answer; // 최대 100001
    }
}
