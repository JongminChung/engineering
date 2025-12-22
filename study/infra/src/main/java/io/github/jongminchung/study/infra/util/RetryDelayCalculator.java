package io.github.jongminchung.study.infra.util;

public final class RetryDelayCalculator {

    private RetryDelayCalculator() {}

    public static long exponentialBackoffMillis(long baseDelayMillis, int attempt, long maxDelayMillis) {
        if (baseDelayMillis < 0) {
            throw new IllegalArgumentException("baseDelayMillis must be >= 0");
        }
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must be >= 0");
        }
        if (maxDelayMillis < 0) {
            throw new IllegalArgumentException("maxDelayMillis must be >= 0");
        }

        if (maxDelayMillis == 0 || baseDelayMillis == 0) {
            return 0;
        }

        long delay = baseDelayMillis;
        for (int i = 0; i < attempt; i++) {
            if (delay >= maxDelayMillis / 2) {
                return maxDelayMillis;
            }
            delay *= 2;
        }

        return Math.min(delay, maxDelayMillis);
    }
}
