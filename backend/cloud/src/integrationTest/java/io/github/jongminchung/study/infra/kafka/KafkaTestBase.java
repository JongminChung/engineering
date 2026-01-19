package io.github.jongminchung.study.infra.kafka;

import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Kafka 학습을 위한 Base Test 클래스 - TestContainers 기반 Kafka 통합 테스트 환경 제공 */
public abstract class KafkaTestBase {

    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.0");

    protected static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE).withReuse(true);

    protected String bootstrapServers;

    static {
        if (!kafka.isRunning()) {
            kafka.start();
        }
    }

    @BeforeEach
    void setUp() {
        bootstrapServers = kafka.getBootstrapServers();
    }

    protected static String getBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    protected void createTopic(String name, int partitions) {
        createTopic(name, partitions, Map.of());
    }

    protected void createTopic(String name, int partitions, Map<String, String> configs) {
        try (AdminClient adminClient =
                AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            NewTopic topic = new NewTopic(name, partitions, (short) 1);
            if (!configs.isEmpty()) {
                topic.configs(configs);
            }
            adminClient.createTopics(java.util.List.of(topic)).all().get();
        } catch (org.apache.kafka.common.errors.TopicExistsException ignored) {
            // ignore
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create topic: " + name, ex);
        }
    }

    /** Producer 기본 설정 생성 */
    protected Properties createProducerProperties() {
        var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    /** Consumer 기본 설정 생성 */
    protected Properties createConsumerProperties(String groupId) {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
