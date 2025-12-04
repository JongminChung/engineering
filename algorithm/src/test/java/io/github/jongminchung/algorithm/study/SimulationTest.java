package io.github.jongminchung.algorithm.study;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationTest {

    @ParameterizedTest(name = "toMinutes({0}) = {1}")
    @CsvSource("""
            00:00, 0,
            00:01, 1,
            00:59, 59,
            01:00, 60,
            23:59, 1439
            """)
    void toMinutesTest(String time, int expected) {
        var parts = time.split(":");

        var h = Integer.parseInt(parts[0]) * 60;
        var m = Integer.parseInt(parts[1]);
        assertThat(h + m).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource("""
            09:30, 18:30
            """)
    void diffTime(String start, String end) {
        var startTime = LocalTime.parse(start);
        var endTime = LocalTime.parse(end);

        var diff = Duration.between(startTime, endTime);
        var minutes = diff.toMinutes();

        assertThat(minutes).isEqualTo(540L);
    }

    @ParameterizedTest
    @CsvSource("""
            5, 7, 05:07
            5, 30, 05:30
            12, 1, 12:01
            18, 30, 18:30
            """)
    void timeFormatting(int hour, int minute, String expected) {
        var formatted = String.format("%02d:%02d", hour, minute);
        assertThat(formatted).isEqualTo(expected);
    }
}
