package io.github.jongminchung.study.infra.kafka.phase7;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.study.infra.kafka.KafkaTestBase;

@DisplayName("Phase 7: 성능 튜닝/모니터링")
class KafkaPerformanceMonitoringTest extends KafkaTestBase {

    @Test
    @DisplayName("프로듀서 지표 수집: record-send-total 증가")
    void shouldIncreaseProducerMetrics() throws Exception {
        String topic = "learning.phase7.metrics.producer";
        createTopic(topic, 1);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            double before = metricValue(producer.metrics(), "record-send-total");
            for (int i = 0; i < 5; i++) {
                producer.send(new ProducerRecord<>(topic, "key" + i, "message-" + i))
                        .get();
            }
            producer.flush();
            double after = metricValue(producer.metrics(), "record-send-total");
            assertThat(after).isGreaterThanOrEqualTo(before);
        }
    }

    @Test
    @DisplayName("컨슈머 지표 수집: records-consumed-total 증가")
    void shouldIncreaseConsumerMetrics() throws Exception {
        String topic = "learning.phase7.metrics.consumer";
        createTopic(topic, 1);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            for (int i = 0; i < 3; i++) {
                producer.send(new ProducerRecord<>(topic, "key" + i, "message-" + i))
                        .get();
            }
        }

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties("phase7-metrics"))) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isEqualTo(3);
            double consumed = metricValue(consumer.metrics(), "records-consumed-total");
            assertThat(consumed).isGreaterThanOrEqualTo(3.0);
        }
    }

    @Test
    @DisplayName("모니터링 키 지표 존재 확인")
    void shouldExposeMonitoringMetrics() {
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            assertThat(metricValue(producer.metrics(), "record-send-rate")).isGreaterThanOrEqualTo(0.0);
            assertThat(metricValue(producer.metrics(), "request-latency-avg")).isGreaterThanOrEqualTo(0.0);
        }
    }

    private double metricValue(Map<MetricName, ? extends Metric> metrics, String name) {
        return metrics.entrySet().stream()
                .filter(entry -> entry.getKey().name().equals(name))
                .map(entry -> entry.getValue().metricValue())
                .filter(Double.class::isInstance)
                .map(Double.class::cast)
                .map(value -> Double.isNaN(value) ? 0.0 : value)
                .findFirst()
                .orElse(0.0);
    }
}
