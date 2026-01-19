# Kafka 학습/기여 통합 가이드

이 문서는 `backend/cloud` 모듈의 Kafka 학습과 오픈소스 기여 준비를 위한 최소 정보를 통합한 문서입니다.
불필요한 예시 코드나 장황한 설정 설명은 제외했습니다.

## 목적

- Spring Kafka로 실전 패턴(DLQ/Retry/동시성/배치)을 먼저 학습
- Kafka 클라이언트와 본체 기여로 연결되는 기반을 확보

## 빠른 시작

- 예시 앱: `backend/cloud/src/main/java/io/github/jongminchung/study/cloud/CloudApplication.java`
- 프로파일 실행:

```bash
SPRING_PROFILES_ACTIVE=kafka-learning \
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9094 \
./gradlew :backend:cloud:bootRun
```

- 통합 테스트(도커 필요):

```bash
./gradlew :backend:cloud:integrationTest
```

공식 문서:

- Apache Kafka Quickstart: https://kafka.apache.org/quickstart
- Spring Boot Kafka: https://docs.spring.io/spring-boot/reference/messaging/kafka.html

## 학습 로드맵(요약) + 테스트 링크

1. 기본 개념/환경: 브로커, 토픽, 파티션, 오프셋
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase1/KafkaBasicTest.java`
2. Producer/Consumer 기본 + 고급 설정
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase2/KafkaProducerConsumerAdvancedTest.java`
3. Offset 전략과 재처리(재시도/복구)
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase3/KafkaOffsetStrategyTest.java`
4. Topic/Partition 설계 및 Retention
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase4/KafkaTopicRetentionTest.java`
5. 메시지 처리 패턴(DLQ/Retry)
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase5/KafkaDlqErrorHandlerTest.java`
6. Spring Kafka 통합(컨테이너 설정, 배치/동시성)
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase6/KafkaListenerContainerTest.java`
7. 성능 튜닝/모니터링
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase7/KafkaPerformanceMonitoringTest.java`
8. 운영/보안/BP
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase8/KafkaOpsSecurityBestPracticeTest.java`

공식 문서:

- Kafka Documentation: https://kafka.apache.org/documentation/

## 공식 문서 기반 단계별 학습 및 테스트 매핑

1. 기본 개념/환경
    - Kafka 공식 문서: https://kafka.apache.org/documentation/
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase1/KafkaBasicTest.java`
2. Producer/Consumer 설정
    - Kafka 공식 문서(Producer): https://kafka.apache.org/documentation/#producerconfigs
    - Kafka 공식 문서(Consumer): https://kafka.apache.org/documentation/#consumerconfigs
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase2/KafkaProducerConsumerAdvancedTest.java`
3. Offset 전략과 재처리
    - Kafka 공식 문서: https://kafka.apache.org/documentation/#semantics
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase3/KafkaOffsetStrategyTest.java`
4. Topic/Partition/Retention
    - Kafka 공식 문서(Topic): https://kafka.apache.org/documentation/#basic_ops
    - Kafka 공식 문서(Topic Config): https://kafka.apache.org/documentation/#topicconfigs
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase4/KafkaTopicRetentionTest.java`
5. 메시지 처리 패턴(DLQ/Retry)
    - Kafka 공식 문서(Offset/재처리): https://kafka.apache.org/documentation/#semantics
    - Spring Kafka 문서(Error Handling): https://docs.spring.io/spring-kafka/reference/html/#error-handling
    - Spring Kafka 문서(DLQ): https://docs.spring.io/spring-kafka/reference/html/#dead-letters
    - Spring Kafka 문서(Retry): https://docs.spring.io/spring-kafka/reference/html/#retrying-deliveries
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase5/KafkaDlqErrorHandlerTest.java`
6. Spring Kafka 컨테이너 설정
    - Spring Kafka 문서(컨테이너): https://docs.spring.io/spring-kafka/reference/html/#message-listener-container
    - Spring Kafka 문서(배치): https://docs.spring.io/spring-kafka/reference/html/#batch-listeners
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase6/KafkaListenerContainerTest.java`
7. 성능 튜닝/모니터링
    - Kafka 공식 문서(프로듀서 튜닝): https://kafka.apache.org/documentation/#producerconfigs
    - Kafka 공식 문서(컨슈머 튜닝): https://kafka.apache.org/documentation/#consumerconfigs
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase7/KafkaPerformanceMonitoringTest.java`
8. 운영/보안/BP
    - Kafka 공식 문서(보안): https://kafka.apache.org/documentation/#security
    - Kafka 공식 문서(Best Practices): https://kafka.apache.org/documentation/#bestpractice
    - 테스트: `backend/cloud/src/integrationTest/java/io/github/jongminchung/study/infra/kafka/phase8/KafkaOpsSecurityBestPracticeTest.java`

## 성능 지표 수집/비교 제안

1. 동일 워크로드로 2개 설정을 비교
    - 예: `linger.ms=0` vs `linger.ms=20`, `batch.size=16384` vs `65536`
2. 클라이언트 메트릭 수집
    - Producer: `record-send-rate`, `batch-size-avg`, `request-latency-avg`
    - Consumer: `records-consumed-rate`, `fetch-rate`, `fetch-size-avg`
3. 비교 방법
    - 동일한 메시지 수/크기를 전송한 뒤 메트릭 스냅샷 비교
    - 차이가 큰 항목을 중심으로 튜닝 반복

## Kafka 본체 기여 로드맵(요약)

1. Spring Kafka로 문제 재현/원인 추적 경험 쌓기
2. Kafka 클라이언트 API로 동일 시나리오 문서화
3. Kafka JIRA 이슈 재현 -> 수정 -> PR 제출

공식 문서:

- Kafka 기여 가이드: https://kafka.apache.org/contributing.html
- Kafka JIRA: https://issues.apache.org/jira/projects/KAFKA/issues
- Spring Kafka 공식 문서: https://docs.spring.io/spring-kafka/reference/html/
