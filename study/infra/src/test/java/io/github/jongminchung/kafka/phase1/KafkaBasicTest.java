package io.github.jongminchung.kafka.phase1;

import io.github.jongminchung.kafka.KafkaTestBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1: 기본 개념 및 환경 구성
 * - Kafka 아키텍처 이해 (Broker, Topic, Partition, Replica)
 * - Producer, Consumer, Consumer Group 개념
 */
@DisplayName("Phase 1: Kafka 기본 개념 학습")
class KafkaBasicTest extends KafkaTestBase {

    @Test
    @DisplayName("1.1 간단한 메시지 전송 및 수신")
    void testSimpleProducerConsumer() throws Exception {
        String topic = "test-topic";
        String testMessage = "Hello Kafka!";

        // Producer로 메시지 전송
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key1", testMessage);
            producer.send(record).get(); // 동기 전송
        }

        // Consumer로 메시지 수신
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties("test-group"))) {
            consumer.subscribe(Collections.singletonList(topic));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isEqualTo(1);

            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.value()).isEqualTo(testMessage);
            assertThat(record.key()).isEqualTo("key1");
        }
    }

    @Test
    @DisplayName("1.2 여러 메시지 전송 및 순서 확인")
    void testMultipleMessages() throws Exception {
        String topic = "multi-message-topic";

        // 여러 메시지 전송
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            for (int i = 0; i < 5; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topic,
                        "key" + i,
                        "message-" + i
                );
                producer.send(record).get();
            }
        }

        // 메시지 수신 및 순서 확인
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties("multi-group"))) {
            consumer.subscribe(Collections.singletonList(topic));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isEqualTo(5);

            int messageIndex = 0;
            for (ConsumerRecord<String, String> record : records) {
                assertThat(record.value()).isEqualTo("message-" + messageIndex);
                assertThat(record.key()).isEqualTo("key" + messageIndex);
                messageIndex++;
            }
        }
    }

    @Test
    @DisplayName("1.3 동일한 Key를 가진 메시지는 동일한 Partition으로")
    void testPartitioningBySameKey() throws Exception {
        String topic = "partition-test-topic";
        String sameKey = "same-key";

        // 동일한 Key로 여러 메시지 전송
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(createProducerProperties())) {
            for (int i = 0; i < 10; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topic,
                        sameKey,
                        "message-" + i
                );
                producer.send(record).get();
            }
        }

        // 메시지 수신 및 Partition 확인
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties("partition-group"))) {
            consumer.subscribe(Collections.singletonList(topic));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isEqualTo(10);

            // 모든 메시지가 동일한 Partition에 있는지 확인
            Integer firstPartition = null;
            for (ConsumerRecord<String, String> record : records) {
                if (firstPartition == null) {
                    firstPartition = record.partition();
                }
                assertThat(record.partition()).isEqualTo(firstPartition);
                assertThat(record.key()).isEqualTo(sameKey);
            }
        }
    }
}
