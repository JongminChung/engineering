# Kafka 학습 로드맵

## 학습 환경 구성

- Docker Compose를 이용한 Kafka (KRaft 모드) + Kafka UI 구성
- TestContainers를 활용한 통합 테스트 환경

---

## Phase 1: 기본 개념 및 환경 구성

### 1.1 Kafka 핵심 개념

- [ ] Kafka 아키텍처 이해 (Broker, Topic, Partition, Replica)
- [ ] KRaft 모드 vs ZooKeeper 모드 차이점
- [ ] Producer, Consumer, Consumer Group 개념
- [ ] Offset 관리 메커니즘

### 1.2 개발 환경 설정

- [ ] Docker Compose로 Kafka KRaft 클러스터 구성
- [ ] Kafka UI 연동 및 모니터링
- [ ] 기본 토픽 생성 및 확인

---

## Phase 2: Producer 학습

### 2.1 기본 Producer

- [ ] 간단한 메시지 전송 테스트
- [ ] Serialization (StringSerializer, JsonSerializer)
- [ ] 동기/비동기 전송 방식 차이

### 2.2 Producer 고급 설정

- [ ] acks 설정 (0, 1, all)
- [ ] Batch 처리 (batch.size, linger.ms)
- [ ] Compression (gzip, snappy, lz4)
- [ ] Idempotent Producer (멱등성)
- [ ] Partitioner 커스터마이징

### 2.3 오류 처리

- [ ] Retry 메커니즘
- [ ] 전송 실패 핸들링
- [ ] Callback을 통한 에러 처리

---

## Phase 3: Consumer 학습

### 3.1 기본 Consumer

- [ ] 메시지 수신 및 처리
- [ ] Deserialization
- [ ] Poll 메커니즘 이해

### 3.2 Consumer Group

- [ ] Consumer Group 동작 원리
- [ ] Partition Rebalancing
- [ ] Group Coordinator 역할

### 3.3 Offset 관리

- [ ] Auto Commit vs Manual Commit
- [ ] Commit 전략 (at-least-once, at-most-once, exactly-once)
- [ ] Offset Reset 정책 (earliest, latest, none)
- [ ] Seek을 이용한 특정 Offset 읽기

### 3.4 Consumer 고급 설정

- [ ] fetch.min.bytes, fetch.max.wait.ms
- [ ] max.poll.records, max.poll.interval.ms
- [ ] session.timeout.ms, heartbeat.interval.ms

---

## Phase 4: Topic & Partition 관리

### 4.1 Topic 설계

- [ ] Topic 생성 및 설정
- [ ] Partition 개수 결정 기준
- [ ] Replication Factor 설정

### 4.2 Partition 전략

- [ ] Key 기반 파티셔닝
- [ ] Round-robin 파티셔닝
- [ ] Custom Partitioner 구현
- [ ] Partition 순서 보장

### 4.3 Retention 정책

- [ ] Time-based Retention
- [ ] Size-based Retention
- [ ] Compaction 전략

---

## Phase 5: 메시지 처리 패턴

### 5.1 기본 패턴

- [ ] Fire and Forget
- [ ] Synchronous Send
- [ ] Asynchronous Send with Callback

### 5.2 고급 패턴

- [ ] Request-Reply 패턴
- [ ] Dead Letter Queue (DLQ)
- [ ] Retry Topic 패턴
- [ ] Event Sourcing

### 5.3 트랜잭션

- [ ] Kafka Transaction API
- [ ] Exactly-once Semantics (EOS)
- [ ] Transactional Producer
- [ ] Transactional Consumer

---

## Phase 6: Spring Kafka 통합

### 6.1 기본 설정

- [ ] Spring Kafka 의존성 추가
- [ ] KafkaTemplate 사용
- [ ] @KafkaListener 활용

### 6.2 고급 기능

- [ ] Error Handler 설정
- [ ] Retry & Recovery
- [ ] Batch Listener
- [ ] Concurrent Consumer

### 6.3 테스트

- [ ] @EmbeddedKafka 테스트
- [ ] TestContainers Kafka 모듈
- [ ] 통합 테스트 작성

---

## Phase 7: 성능 최적화 & 모니터링

### 7.1 성능 튜닝

- [ ] Producer 성능 최적화
- [ ] Consumer 성능 최적화
- [ ] Network & IO 최적화
- [ ] JVM 튜닝

### 7.2 모니터링

- [ ] Kafka UI로 메트릭 확인
- [ ] JMX 메트릭 수집
- [ ] Lag 모니터링
- [ ] Throughput 측정

### 7.3 트러블슈팅

- [ ] Consumer Lag 문제 해결
- [ ] Rebalancing 문제 분석
- [ ] Out of Memory 문제
- [ ] Network 타임아웃 이슈

---

## Phase 8: 운영 및 Best Practices

### 8.1 운영 가이드

- [ ] Topic 관리 전략
- [ ] Schema Evolution
- [ ] 버전 업그레이드
- [ ] Backup & Recovery

### 8.2 보안

- [ ] Authentication (SASL)
- [ ] Authorization (ACL)
- [ ] Encryption (SSL/TLS)

### 8.3 Best Practices

- [ ] 메시지 크기 최적화
- [ ] Key 설계 전략
- [ ] Error Handling 패턴
- [ ] 모니터링 체크리스트

---

## 실습 프로젝트 아이디어

1. **이벤트 로깅 시스템**: 애플리케이션 이벤트를 Kafka로 전송하고 수집
2. **주문 처리 시스템**: 주문 생성 → 결제 → 배송 이벤트 처리
3. **실시간 알림 시스템**: 사용자 활동 기반 실시간 알림 발송
4. **데이터 파이프라인**: 여러 소스의 데이터를 Kafka를 통해 통합
5. **Change Data Capture (CDC)**: DB 변경 사항을 Kafka로 스트리밍

---

## 학습 리소스

- [Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Confluent Developer](https://developer.confluent.io/)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/html/)
- TestContainers Kafka 모듈 문서

---

## 다음 단계

1. Docker Compose 파일 작성 및 Kafka 클러스터 실행
2. 첫 번째 Producer/Consumer 테스트 코드 작성
3. 각 Phase별 실습 진행
