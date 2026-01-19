package io.github.jongminchung.study.infra.kafka.phase3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.study.infra.kafka.KafkaTestBase;

@DisplayName("Phase 3: Offset 전략과 재처리")
class KafkaOffsetStrategyTest extends KafkaTestBase {

    @Test
    @DisplayName("수동 커밋 미수행 시 재처리")
    void shouldReprocessWhenNotCommitted() throws Exception {
        String topic = "learning.phase3.manual";
        createTopic(topic, 1);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            producer.send(new ProducerRecord<>(topic, "key", "manual-message")).get();
        }

        Properties manualProps = createConsumerProperties("phase3-manual");
        manualProps.put("enable.auto.commit", "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(manualProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isEqualTo(1);
        }

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(manualProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("자동 커밋 후 재처리되지 않음")
    void shouldNotReprocessAfterAutoCommit() throws Exception {
        String topic = "learning.phase3.auto";
        createTopic(topic, 1);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            producer.send(new ProducerRecord<>(topic, "key", "auto-message")).get();
        }

        Properties autoProps = createConsumerProperties("phase3-auto");
        autoProps.put("enable.auto.commit", "true");
        autoProps.put("auto.commit.interval.ms", "500");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(autoProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isEqualTo(1);
            Thread.sleep(600);
        }

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(autoProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
            assertThat(records.count()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("seek으로 오프셋 재설정")
    void shouldSeekToBeginning() throws Exception {
        String topic = "learning.phase3.seek";
        createTopic(topic, 1);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            producer.send(new ProducerRecord<>(topic, "key", "seek-message")).get();
        }

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties("phase3-seek"))) {
            TopicPartition partition = new TopicPartition(topic, 0);
            consumer.assign(Collections.singletonList(partition));
            consumer.seekToBeginning(Collections.singletonList(partition));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isEqualTo(1);

            consumer.seekToBeginning(Collections.singletonList(partition));
            ConsumerRecords<String, String> reread = consumer.poll(Duration.ofSeconds(5));
            assertThat(reread.count()).isEqualTo(1);
        }
    }
}
