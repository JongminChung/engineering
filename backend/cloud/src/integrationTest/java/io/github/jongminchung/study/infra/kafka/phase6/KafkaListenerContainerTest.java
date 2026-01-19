package io.github.jongminchung.study.infra.kafka.phase6;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.github.jongminchung.study.infra.kafka.KafkaTestBase;

@SpringJUnitConfig(classes = KafkaListenerContainerTest.KafkaListenerContainerTestApplication.class)
@DisplayName("Phase 6: KafkaListener 컨테이너 설정 통합 테스트")
class KafkaListenerContainerTest extends KafkaTestBase {

    private static final String CONCURRENT_TOPIC = "learning.concurrent";
    private static final String BATCH_TOPIC = "learning.batch";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConcurrentListener concurrentListener;

    @Autowired
    private BatchListener batchListener;

    @BeforeEach
    void resetListeners() {
        concurrentListener.reset();
        batchListener.reset();
    }

    @Test
    @DisplayName("동시성 컨테이너로 파티션별 메시지를 처리")
    void shouldConsumeMessagesWithConcurrency() throws Exception {
        kafkaTemplate.send(CONCURRENT_TOPIC, 0, "key-0", "message-0");
        kafkaTemplate.send(CONCURRENT_TOPIC, 1, "key-1", "message-1");

        assertThat(concurrentListener.await(Duration.ofSeconds(10))).isTrue();
        assertThat(concurrentListener.partitions()).containsExactlyInAnyOrder(0, 1);
    }

    @Test
    @DisplayName("배치 리스너가 여러 메시지를 묶어서 처리")
    void shouldConsumeBatchMessages() {
        kafkaTemplate.send(BATCH_TOPIC, "batch-1");
        kafkaTemplate.send(BATCH_TOPIC, "batch-2");
        kafkaTemplate.send(BATCH_TOPIC, "batch-3");

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(batchListener.totalMessages()).isEqualTo(3);
            assertThat(batchListener.batchCount()).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    @DisplayName("max.poll.records 설정에 따라 배치가 분리됨")
    void shouldSplitBatchesByMaxPollRecords() {
        kafkaTemplate.send(BATCH_TOPIC, "batch-4");
        kafkaTemplate.send(BATCH_TOPIC, "batch-5");
        kafkaTemplate.send(BATCH_TOPIC, "batch-6");

        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(batchListener.batchCount())
                .isGreaterThanOrEqualTo(2));
    }

    @EnableKafka
    @Configuration
    static class KafkaListenerContainerTestApplication {

        @Bean
        KafkaAdmin kafkaAdmin() {
            var configs = new HashMap<String, Object>();
            configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestBase.getBootstrapServers());
            return new KafkaAdmin(configs);
        }

        @Bean
        ProducerFactory<String, String> producerFactory() {
            var configs = new HashMap<String, Object>();
            configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestBase.getBootstrapServers());
            configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(configs);
        }

        @Bean
        ConsumerFactory<String, String> consumerFactory() {
            var configs = new HashMap<String, Object>();
            configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestBase.getBootstrapServers());
            configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2);
            return new DefaultKafkaConsumerFactory<>(configs);
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        NewTopic concurrentTopic() {
            return new NewTopic(CONCURRENT_TOPIC, 2, (short) 1);
        }

        @Bean
        NewTopic batchTopic() {
            return new NewTopic(BATCH_TOPIC, 1, (short) 1);
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory) {
            var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
            factory.setConsumerFactory(consumerFactory);
            factory.setConcurrency(2);
            return factory;
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory) {
            var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
            factory.setConsumerFactory(consumerFactory);
            factory.setBatchListener(true);
            return factory;
        }

        @Bean
        ConcurrentListener concurrentListener() {
            return new ConcurrentListener();
        }

        @Bean
        BatchListener batchListener() {
            return new BatchListener();
        }
    }

    static class ConcurrentListener {

        private final Set<Integer> partitions = Collections.synchronizedSet(new HashSet<>());
        private CountDownLatch latch = new CountDownLatch(2);

        @KafkaListener(
                topics = CONCURRENT_TOPIC,
                groupId = "concurrent-handler",
                containerFactory = "concurrentKafkaListenerContainerFactory")
        void onMessage(String payload, org.springframework.messaging.MessageHeaders headers) {
            Object partition = headers.get(KafkaHeaders.RECEIVED_PARTITION);
            if (partition instanceof Integer) {
                partitions.add((Integer) partition);
            }
            latch.countDown();
        }

        void reset() {
            partitions.clear();
            latch = new CountDownLatch(2);
        }

        boolean await(Duration timeout) throws InterruptedException {
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        Set<Integer> partitions() {
            return partitions;
        }
    }

    static class BatchListener {

        private final List<List<String>> batches = Collections.synchronizedList(new ArrayList<>());

        @KafkaListener(
                topics = BATCH_TOPIC,
                groupId = "batch-handler",
                containerFactory = "batchKafkaListenerContainerFactory")
        void onMessages(List<String> payloads) {
            batches.add(payloads);
        }

        void reset() {
            batches.clear();
        }

        int totalMessages() {
            return batches.stream().mapToInt(List::size).sum();
        }

        int batchCount() {
            return batches.size();
        }
    }
}
