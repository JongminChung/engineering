package io.github.jongminchung.study.infra.kafka.phase4;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.study.infra.kafka.KafkaTestBase;

@DisplayName("Phase 4: Topic/Partition 설계 및 Retention")
class KafkaTopicRetentionTest extends KafkaTestBase {

    @Test
    @DisplayName("파티션 개수 확인")
    void shouldCreateTopicWithPartitions() throws Exception {
        String topic = "learning.phase4.partitions";
        createTopic(topic, 3);

        try (AdminClient adminClient =
                AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            DescribeTopicsResult result = adminClient.describeTopics(java.util.List.of(topic));
            TopicDescription description =
                    result.allTopicNames().get(5, TimeUnit.SECONDS).get(topic);
            assertThat(description.partitions()).hasSize(3);
        }
    }

    @Test
    @DisplayName("retention.ms 설정 확인")
    void shouldApplyRetentionMsConfig() throws Exception {
        String topic = "learning.phase4.retention";
        createTopic(topic, 1, Map.of("retention.ms", "1000"));

        Config config = describeTopicConfig(topic);
        ConfigEntry retention = config.get("retention.ms");
        assertThat(retention.value()).isEqualTo("1000");
    }

    @Test
    @DisplayName("cleanup.policy=compact 확인")
    void shouldApplyCompactionPolicy() throws Exception {
        String topic = "learning.phase4.compaction";
        createTopic(topic, 1, Map.of("cleanup.policy", "compact"));

        Config config = describeTopicConfig(topic);
        ConfigEntry policy = config.get("cleanup.policy");
        assertThat(policy.value()).contains("compact");
    }

    private Config describeTopicConfig(String topic) throws Exception {
        try (AdminClient adminClient =
                AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
            AtomicReference<Config> config = new AtomicReference<>();

            Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
                try {
                    Config resolved = adminClient
                            .describeConfigs(java.util.List.of(resource))
                            .all()
                            .get(2, TimeUnit.SECONDS)
                            .get(resource);
                    config.set(resolved);
                } catch (Exception ex) {
                    throw new AssertionError("Topic config not ready yet", ex);
                }
            });

            return config.get();
        }
    }
}
