package io.github.jongminchung.study.infra.kafka.phase5;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
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
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.backoff.FixedBackOff;

import io.github.jongminchung.study.infra.kafka.KafkaTestBase;

@SpringJUnitConfig(classes = KafkaDlqErrorHandlerTest.KafkaDlqTestApplication.class)
@DisplayName("Phase 5: DLQ/ErrorHandler 통합 테스트")
class KafkaDlqErrorHandlerTest extends KafkaTestBase {

    private static final String INPUT_TOPIC = "learning.dlq.input";
    private static final String DLQ_TOPIC = "learning.dlq";
    private static final String RETRY_TOPIC = "learning.retry.input";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private FailingListener failingListener;

    @Autowired
    private RetryingListener retryingListener;

    @Test
    @DisplayName("DLQ로 메시지가 라우팅되고 헤더가 기록되는지 검증")
    void shouldSendToDlqWhenListenerFails() throws Exception {
        String payload = "fail-message-1";
        kafkaTemplate.send(new ProducerRecord<>(INPUT_TOPIC, "key", payload)).get();

        assertThat(failingListener.awaitMessage(Duration.ofSeconds(10))).isTrue();

        ConsumerRecord<String, String> record = awaitDlqRecord(payload, "dlq-consumer-1");
        assertThat(record.value()).contains(payload);
        assertThat(readStringHeader(record, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(INPUT_TOPIC);
        assertThat(readIntHeader(record, KafkaHeaders.DLT_ORIGINAL_PARTITION)).isGreaterThanOrEqualTo(0);
        assertThat(readLongHeader(record, KafkaHeaders.DLT_ORIGINAL_OFFSET)).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("DLQ에 예외 정보 헤더가 포함되는지 검증")
    void shouldIncludeExceptionHeaders() throws Exception {
        String payload = "fail-message-2";
        kafkaTemplate.send(new ProducerRecord<>(INPUT_TOPIC, "key", payload)).get();

        ConsumerRecord<String, String> record = awaitDlqRecord(payload, "dlq-consumer-2");
        assertThat(readStringHeader(record, KafkaHeaders.DLT_EXCEPTION_FQCN)).isNotBlank();
        assertThat(readStringHeader(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).contains(payload);
    }

    @Test
    @DisplayName("Retry/Backoff 이후 정상 처리되는지 검증")
    void shouldRetryWithBackoffAndEventuallySucceed() throws Exception {
        kafkaTemplate.send(RETRY_TOPIC, "key", "retry-message");

        assertThat(retryingListener.awaitSuccess(Duration.ofSeconds(20))).isTrue();
        assertThat(retryingListener.attempts()).isEqualTo(3);
        assertThat(retryingListener.hasBackoff(Duration.ofMillis(200))).isTrue();
    }

    @EnableKafka
    @Configuration
    static class KafkaDlqTestApplication {

        @Bean
        KafkaAdmin kafkaAdmin() {
            var configs = new java.util.HashMap<String, Object>();
            configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestBase.getBootstrapServers());
            return new KafkaAdmin(configs);
        }

        @Bean
        ProducerFactory<String, String> producerFactory() {
            var configs = new java.util.HashMap<String, Object>();
            configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestBase.getBootstrapServers());
            configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(configs);
        }

        @Bean
        ConsumerFactory<String, String> consumerFactory() {
            var configs = new java.util.HashMap<String, Object>();
            configs.put(
                    org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    KafkaTestBase.getBootstrapServers());
            configs.put(
                    org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer.class);
            configs.put(
                    org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    StringDeserializer.class);
            configs.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            return new DefaultKafkaConsumerFactory<>(configs);
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        NewTopic inputTopic() {
            return new NewTopic(INPUT_TOPIC, 1, (short) 1);
        }

        @Bean
        NewTopic dlqTopic() {
            return new NewTopic(DLQ_TOPIC, 1, (short) 1);
        }

        @Bean
        NewTopic retryTopic() {
            return new NewTopic(RETRY_TOPIC, 1, (short) 1);
        }

        @Bean
        DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
            var recoverer = new DeadLetterPublishingRecoverer(
                    kafkaTemplate, (record, ex) -> new TopicPartition(DLQ_TOPIC, record.partition()));
            return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler kafkaErrorHandler) {
            var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
            factory.setConsumerFactory(consumerFactory);
            factory.setCommonErrorHandler(kafkaErrorHandler);
            return factory;
        }

        @Bean
        DefaultErrorHandler retryErrorHandler() {
            return new DefaultErrorHandler(new FixedBackOff(200L, 2L));
        }

        @Bean
        ConcurrentKafkaListenerContainerFactory<String, String> retryKafkaListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler retryErrorHandler) {
            var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
            factory.setConsumerFactory(consumerFactory);
            factory.setCommonErrorHandler(retryErrorHandler);
            return factory;
        }

        @Bean
        FailingListener failingListener() {
            return new FailingListener();
        }

        @Bean
        RetryingListener retryingListener() {
            return new RetryingListener();
        }
    }

    static class FailingListener {

        private final CountDownLatch latch = new CountDownLatch(1);

        @KafkaListener(topics = INPUT_TOPIC, groupId = "dlq-handler")
        void onMessage(String payload) {
            latch.countDown();
            throw new IllegalStateException("force dlq: " + payload);
        }

        boolean awaitMessage(Duration timeout) throws InterruptedException {
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    static class RetryingListener {

        private final AtomicInteger attempts = new AtomicInteger();
        private final Queue<Long> attemptTimes = new ConcurrentLinkedQueue<>();
        private final CountDownLatch successLatch = new CountDownLatch(1);

        @KafkaListener(
                topics = RETRY_TOPIC,
                groupId = "retry-handler",
                containerFactory = "retryKafkaListenerContainerFactory")
        void onMessage(String payload) {
            attemptTimes.add(System.nanoTime());
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("force retry: " + payload);
            }
            successLatch.countDown();
        }

        boolean awaitSuccess(Duration timeout) throws InterruptedException {
            return successLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        int attempts() {
            return attempts.get();
        }

        boolean hasBackoff(Duration minimumBackoff) {
            Long first = attemptTimes.poll();
            Long second = attemptTimes.poll();
            if (first == null || second == null) {
                return false;
            }
            long deltaMillis = TimeUnit.NANOSECONDS.toMillis(second - first);
            return deltaMillis >= minimumBackoff.toMillis();
        }
    }

    private ConsumerRecord<String, String> awaitDlqRecord(String payload, String groupId) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties(groupId))) {
            consumer.subscribe(Collections.singletonList(DLQ_TOPIC));
            AtomicReference<ConsumerRecord<String, String>> found = new AtomicReference<>();

            Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                records.forEach(record -> {
                    if (record.value() != null && record.value().contains(payload)) {
                        found.set(record);
                    }
                });
                assertThat(found.get()).isNotNull();
            });

            return found.get();
        }
    }

    private static String readStringHeader(ConsumerRecord<String, String> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value());
    }

    private static int readIntHeader(ConsumerRecord<String, String> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) {
            return -1;
        }
        return ByteBuffer.wrap(header.value()).getInt();
    }

    private static long readLongHeader(ConsumerRecord<String, String> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) {
            return -1L;
        }
        return ByteBuffer.wrap(header.value()).getLong();
    }
}
