package io.github.jongminchung.study.infra.kafka.phase8;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.study.infra.kafka.KafkaTestBase;

@DisplayName("Phase 8: 운영/보안/Best Practices")
class KafkaOpsSecurityBestPracticeTest extends KafkaTestBase {

    @Test
    @DisplayName("메시지 크기 제한")
    void shouldRejectTooLargeMessage() {
        String topic = "learning.phase8.size";
        createTopic(topic, 1);

        Properties props = createProducerProperties();
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 256);

        String largePayload = "x".repeat(1024);

        assertThatThrownBy(() -> {
                    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
                        producer.send(new ProducerRecord<>(topic, "key", largePayload))
                                .get(3, TimeUnit.SECONDS);
                    }
                })
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("SSL 설정 미일치 시 실패")
    void shouldFailWithSslConfig() {
        Properties props = createProducerProperties();
        props.put("security.protocol", "SSL");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);

        assertThatThrownBy(() -> {
                    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
                        producer.send(new ProducerRecord<>("learning.phase8.ssl", "key", "message"))
                                .get(3, TimeUnit.SECONDS);
                    }
                })
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("SASL 설정 미일치 시 실패")
    void shouldFailWithSaslConfig() {
        Properties props = createProducerProperties();
        props.put("security.protocol", "SASL_PLAINTEXT");
        props.put("sasl.mechanism", "PLAIN");
        props.put(
                "sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"user\" password=\"pass\";");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);

        assertThatThrownBy(() -> {
                    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
                        producer.send(new ProducerRecord<>("learning.phase8.sasl", "key", "message"))
                                .get(3, TimeUnit.SECONDS);
                    }
                })
                .isInstanceOf(Exception.class);
    }
}
