package io.github.jongminchung.study.infra.kafka.phase2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.study.infra.kafka.KafkaTestBase;

@DisplayName("Phase 2: Producer/Consumer 기본 + 고급 설정")
class KafkaProducerConsumerAdvancedTest extends KafkaTestBase {

    @Test
    @DisplayName("acks=all로 동기 전송")
    void shouldSendWithAcksAll() throws Exception {
        String topic = "learning.phase2.sync";
        createTopic(topic, 1);

        Properties props = createProducerProperties();
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, "key", "sync-message")).get();
        }

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties("phase2-sync"))) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("비동기 전송 콜백 확인")
    void shouldSendAsyncWithCallback() throws Exception {
        String topic = "learning.phase2.async";
        createTopic(topic, 1);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exception = new AtomicReference<>();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            producer.send(new ProducerRecord<>(topic, "key", "async-message"), (metadata, ex) -> {
                exception.set(ex);
                latch.countDown();
            });
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(exception.get()).isNull();
    }

    @Test
    @DisplayName("max.poll.records 적용")
    void shouldRespectMaxPollRecords() throws Exception {
        String topic = "learning.phase2.poll";
        createTopic(topic, 1);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            for (int i = 0; i < 5; i++) {
                producer.send(new ProducerRecord<>(topic, "key" + i, "message-" + i))
                        .get();
            }
        }

        Properties consumerProps = createConsumerProperties("phase2-poll");
        consumerProps.put("max.poll.records", "2");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isBetween(1, 2);
        }
    }
}
