# Kafka 환경 설정 가이드

## 1. Docker Compose로 Kafka 실행

### 실행 명령어

```bash
# Kafka 클러스터 시작
docker-compose -f docker-compose.kafka.yml up -d

# 로그 확인
docker-compose -f docker-compose.kafka.yml logs -f

# 중지
docker-compose -f docker-compose.kafka.yml down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose -f docker-compose.kafka.yml down -v
```

### 접속 정보

- **Kafka Broker**: `localhost:9094` (외부 접속용)
- **Kafka UI**: http://localhost:8080
- **내부 네트워크**: `kafka:9092` (컨테이너 간 통신용)

---

## 2. Gradle 모듈 생성

### 새로운 Kafka 학습 모듈 추가

```bash
# kafka 모듈 디렉토리 생성
mkdir -p kafka/src/main/java/io/github/jongminchung/kafka
mkdir -p kafka/src/test/java/io/github/jongminchung/kafka
```

### settings.gradle.kts에 모듈 추가

```kotlin
include("app", "list", "utilities", "algorithm", "kafka")
```

---

## 3. Kafka 모듈 의존성 (build.gradle.kts)

### 기본 의존성

```kotlin
plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    // Kafka Client
    implementation("org.apache.kafka:kafka-clients:3.6.1")

    // Spring Kafka (Optional - Spring 사용 시)
    implementation("org.springframework.kafka:spring-kafka:3.1.1")

    // JSON 직렬화
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
}
```

---

## 4. TestContainers 기본 테스트 예제

### 4.1 간단한 Producer/Consumer 테스트

```java
package io.github.jongminchung.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaBasicTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @Test
    void testProducerAndConsumer() throws Exception {
        String topic = "test-topic";
        String testMessage = "Hello Kafka!";

        // Producer 설정
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 메시지 전송
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key", testMessage);
            producer.send(record).get(); // 동기 전송
        }

        // Consumer 설정
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 메시지 수신
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topic));

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isEqualTo(1);

            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.value()).isEqualTo(testMessage);
            assertThat(record.key()).isEqualTo("key");
        }
    }
}
```

### 4.2 Base Test 클래스 (재사용)

```java
package io.github.jongminchung.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;

@Testcontainers
public abstract class KafkaTestBase {

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    ).withReuse(false);

    protected String bootstrapServers;

    @BeforeEach
    void setUp() {
        bootstrapServers = kafka.getBootstrapServers();
    }

    @AfterEach
    void tearDown() {
        // 필요시 정리 작업
    }

    protected Properties createProducerProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    protected Properties createConsumerProperties(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
```

---

## 5. 실습 시작하기

### Step 1: Docker Compose 실행

```bash
docker-compose -f docker-compose.kafka.yml up -d
```

### Step 2: Kafka UI 접속

브라우저에서 http://localhost:8080 접속하여 클러스터 상태 확인

### Step 3: 테스트 코드 실행

```bash
./gradlew :kafka:test
```

### Step 4: 로드맵 따라 학습 진행

`docs/kafka/ROADMAP.md` 참고

---

## 6. 유용한 Docker 명령어

### Kafka 컨테이너 접속

```bash
docker exec -it kafka bash
```

### Topic 생성 (컨테이너 내부)

```bash
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 1
```

### Topic 목록 확인

```bash
kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Topic 상세 정보

```bash
kafka-topics.sh --describe \
  --bootstrap-server localhost:9092 \
  --topic test-topic
```

### Console Producer

```bash
kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic test-topic
```

### Console Consumer

```bash
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --from-beginning
```

---

## 7. 트러블슈팅

### 포트 충돌 시

```bash
# 포트 사용 확인
lsof -i :9092
lsof -i :8080

# 프로세스 종료
kill -9 <PID>
```

### 볼륨 정리

```bash
docker volume prune
```

### 컨테이너 재시작

```bash
docker-compose -f docker-compose.kafka.yml restart
```

---

## 다음 단계

1. `kafka` 모듈 생성
2. 기본 Producer/Consumer 테스트 작성
3. ROADMAP.md의 Phase 1부터 순서대로 학습
