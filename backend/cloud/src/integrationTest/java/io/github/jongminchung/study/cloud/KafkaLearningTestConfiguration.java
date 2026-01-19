package io.github.jongminchung.study.cloud;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@EnableKafka
@TestConfiguration
class KafkaLearningTestConfiguration {

    private static final String TOPIC_BASIC = "learning.basic";
    private static final String TOPIC_ORDERED = "learning.ordered";
    private static final String TOPIC_DLQ = "learning.dlq";

    @Bean
    NewTopic learningBasicTopic() {
        return new NewTopic(TOPIC_BASIC, 3, (short) 1);
    }

    @Bean
    NewTopic learningOrderedTopic() {
        return new NewTopic(TOPIC_ORDERED, 1, (short) 1);
    }

    @Bean
    NewTopic learningDlqTopic() {
        return new NewTopic(TOPIC_DLQ, 1, (short) 1);
    }

    @Bean
    CommandLineRunner kafkaLearningProducer(KafkaTemplate<String, String> kafkaTemplate) {
        return args -> {
            kafkaTemplate.send(TOPIC_BASIC, "key-1", "phase2: async send");
            kafkaTemplate.send(TOPIC_BASIC, "key-2", "phase2: another async send");

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(TOPIC_ORDERED, "order-1", "phase4: partition by key");
            record.headers().add("trace-id", "roadmap".getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(record);
        };
    }

    @Component
    static class KafkaLearningConsumer {

        private static final Logger log = LoggerFactory.getLogger(KafkaLearningConsumer.class);

        @KafkaListener(topics = TOPIC_BASIC, groupId = "learning-basic")
        void onBasicMessage(
                @Payload String message,
                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                @Header(KafkaHeaders.OFFSET) long offset) {
            log.info("phase3: message={}, partition={}, offset={}", message, partition, offset);
        }

        @KafkaListener(topics = TOPIC_ORDERED, groupId = "learning-ordered")
        void onOrderedMessage(@Payload String message) {
            log.info("phase4: ordered message={}", message);
        }
    }
}
