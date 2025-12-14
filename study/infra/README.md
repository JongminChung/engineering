# Kafka 학습 모듈

Kafka 학습을 위한 모듈입니다. TestContainers를 활용하여 실제 Kafka 환경에서 학습할 수 있습니다.

## 시작하기

### 1. Gradle 태스크 확인

```bash
./gradlew :kafka:tasks --group=kafka
```

### 2. Docker Compose로 Kafka 실행

```bash
# Kafka 클러스터 시작
./gradlew :kafka:dockerComposeUp

# Kafka UI 열기
./gradlew :kafka:kafkaUI
# 또는 브라우저에서 http://localhost:8080 접속

# 로그 확인
./gradlew :kafka:dockerComposeLogs

# 중지
./gradlew :kafka:dockerComposeDown

# 볼륨까지 삭제 (데이터 초기화)
./gradlew :kafka:dockerComposeDownVolumes
```

### 3. 테스트 실행

```bash
# 모든 테스트 실행
./gradlew :kafka:test

# 특정 Phase 테스트만 실행
./gradlew :kafka:test --tests "io.github.jongminchung.kafka.phase1.*"
```

## 프로젝트 구조

```
kafka/
├── docker-compose.kafka.yml          # Kafka + Kafka UI 구성
├── build.gradle.kts                  # Kafka 컨벤션 플러그인 적용
├── src/
│   ├── main/java/io/github/jongminchung/kafka/
│   │   ├── producer/                 # Producer 실습 코드
│   │   ├── consumer/                 # Consumer 실습 코드
│   │   └── config/                   # 설정 클래스
│   └── test/java/io/github/jongminchung/kafka/
│       ├── KafkaTestBase.java        # 테스트 Base 클래스
│       ├── phase1/                   # Phase 1: 기본 개념
│       ├── phase2/                   # Phase 2: Producer 학습
│       ├── phase3/                   # Phase 3: Consumer 학습
│       ├── phase4/                   # Phase 4: Topic & Partition
│       ├── phase5/                   # Phase 5: 메시지 처리 패턴
│       ├── phase6/                   # Phase 6: Spring Kafka
│       ├── phase7/                   # Phase 7: 성능 최적화
│       └── phase8/                   # Phase 8: 운영 Best Practices
└── README.md
```

## 학습 로드맵

### Phase 1: 기본 개념 및 환경 구성 ✅
- [x] 간단한 메시지 전송 및 수신
- [x] 여러 메시지 전송 및 순서 확인
- [x] Partition 동작 확인
- 테스트: `phase1/KafkaBasicTest.java`

### Phase 2: Producer 학습
- [ ] 동기/비동기 전송
- [ ] acks 설정 (0, 1, all)
- [ ] Batch 처리
- [ ] Idempotent Producer
- 테스트: `phase2/KafkaProducerTest.java`

### Phase 3: Consumer 학습
- [ ] Poll 메커니즘
- [ ] Consumer Group
- [ ] Offset 관리 (Auto/Manual Commit)
- [ ] Rebalancing
- 테스트: `phase3/KafkaConsumerTest.java`

### Phase 4: Topic & Partition 관리
- [ ] Partition 전략
- [ ] Custom Partitioner
- [ ] Retention 정책
- 테스트: `phase4/TopicPartitionTest.java`

### Phase 5: 메시지 처리 패턴
- [ ] Request-Reply 패턴
- [ ] Dead Letter Queue
- [ ] Kafka Transaction
- [ ] Exactly-once Semantics
- 테스트: `phase5/MessagePatternTest.java`

### Phase 6: Spring Kafka 통합
- [ ] KafkaTemplate
- [ ] @KafkaListener
- [ ] Error Handler
- [ ] Batch Listener
- 테스트: `phase6/SpringKafkaTest.java`

### Phase 7: 성능 최적화 & 모니터링
- [ ] Producer 성능 튜닝
- [ ] Consumer 성능 튜닝
- [ ] Lag 모니터링
- 테스트: `phase7/PerformanceTest.java`

### Phase 8: 운영 및 Best Practices
- [ ] Schema Evolution
- [ ] Security (SASL/SSL)
- [ ] Backup & Recovery
- 테스트: `phase8/OperationTest.java`

## 의존성

build-logic의 `buildlogic.kafka-study-conventions.gradle.kts`에서 관리:
- Kafka Clients: 3.6.1
- Spring Kafka: 3.1.1
- TestContainers Kafka: 1.19.3
- Jackson: 2.16.0
- Awaitility: 4.2.0

## 유용한 명령어

### Kafka CLI (컨테이너 내부)

```bash
# 컨테이너 접속
docker exec -it kafka bash

# Topic 생성
kafka-topics.sh --create --bootstrap-server localhost:9092 \
  --topic my-topic --partitions 3 --replication-factor 1

# Topic 목록
kafka-topics.sh --list --bootstrap-server localhost:9092

# Topic 상세 정보
kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic my-topic

# Console Producer
kafka-console-producer.sh --bootstrap-server localhost:9092 --topic my-topic

# Console Consumer
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic my-topic --from-beginning
```

## 참고 자료

- [Kafka 학습 로드맵](./ROADMAP.md)
- [환경 설정 가이드](./SETUP.md)
- [Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Spring Kafka 문서](https://docs.spring.io/spring-kafka/reference/html/)
