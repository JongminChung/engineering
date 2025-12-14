# Kafka 학습 빠른 시작 가이드

## 프로젝트 구성 완료 ✅

build-logic에 Kafka 학습용 컨벤션 플러그인을 정의하고 kafka 모듈에 적용했습니다.

### 구성 요소

1. **build-logic/src/main/kotlin/buildlogic.kafka-study-conventions.gradle.kts**
   - Kafka Client, Spring Kafka, TestContainers 의존성 자동 설정
   - Docker Compose 관리 Gradle 태스크 제공
   - 테스트 환경 자동 구성

2. **kafka/build.gradle.kts**
   - `buildlogic.kafka-study-conventions` 플러그인 적용
   - 간단한 설정으로 모든 의존성 자동 주입

3. **kafka/docker-compose.kafka.yml**
   - Kafka (KRaft 모드) + Kafka UI 구성
   - 모듈 내부로 이동하여 관리

---

## 사용 가능한 Gradle 태스크

### Kafka 관련 태스크

```bash
# Kafka 클러스터 시작
./gradlew :kafka:dockerComposeUp

# Kafka UI 브라우저에서 열기 (http://localhost:8080)
./gradlew :kafka:kafkaUI

# Kafka 로그 확인
./gradlew :kafka:dockerComposeLogs

# Kafka 클러스터 중지
./gradlew :kafka:dockerComposeDown

# Kafka 클러스터 중지 및 볼륨 삭제 (데이터 초기화)
./gradlew :kafka:dockerComposeDownVolumes
```

### 테스트 실행

```bash
# 모든 테스트 실행
./gradlew :kafka:test

# Phase 1 테스트만 실행
./gradlew :kafka:test --tests "io.github.jongminchung.kafka.phase1.*"

# 특정 테스트 클래스만 실행
./gradlew :kafka:test --tests "io.github.jongminchung.kafka.phase1.KafkaBasicTest"

# 특정 테스트 메서드만 실행
./gradlew :kafka:test --tests "io.github.jongminchung.kafka.phase1.KafkaBasicTest.testSimpleProducerConsumer"
```

---

## 학습 시작하기

### Step 1: Docker Compose로 Kafka 실행 (선택사항)

```bash
./gradlew :kafka:dockerComposeUp
./gradlew :kafka:kafkaUI
```

> **참고**: TestContainers를 사용하므로 Docker Compose 실행은 선택사항입니다.
> 테스트는 자동으로 Kafka 컨테이너를 시작합니다.

### Step 2: Phase 1 테스트 실행

```bash
./gradlew :kafka:test --tests "io.github.jongminchung.kafka.phase1.*"
```

### Step 3: 테스트 코드 확인
- `kafka/src/test/java/io/github/jongminchung/kafka/phase1/KafkaBasicTest.java`
- 3가지 기본 테스트:
  1. 간단한 메시지 전송 및 수신
  2. 여러 메시지 전송 및 순서 확인
  3. 동일한 Key를 가진 메시지의 Partition 동작 확인

### Step 4: 로드맵 따라 학습
`docs/kafka/ROADMAP.md` 참고하여 Phase별로 진행

---

## 프로젝트 구조

```
kafka/
├── docker-compose.kafka.yml          # Kafka + UI 구성
├── build.gradle.kts                  # 컨벤션 플러그인 적용
├── README.md                         # 상세 가이드
├── src/
│   ├── main/java/io/github/jongminchung/kafka/
│   │   └── .gitkeep
│   └── test/java/io/github/jongminchung/kafka/
│       ├── KafkaTestBase.java        # 테스트 Base 클래스
│       └── phase1/                   # Phase 1: 기본 개념
│           └── KafkaBasicTest.java   # ✅ 완성

build-logic/src/main/kotlin/
└── buildlogic.kafka-study-conventions.gradle.kts  # ✅ 컨벤션 플러그인
```

---

## 의존성 (자동 주입됨)

`buildlogic.kafka-study-conventions` 플러그인이 다음 의존성을 자동으로 주입합니다:

### 운영 의존성
- `org.apache.kafka:kafka-clients:3.6.1`
- `org.springframework.kafka:spring-kafka:3.1.1`
- `com.fasterxml.jackson.core:jackson-databind:2.16.0`
- `org.slf4j:slf4j-api:2.0.9`
- `ch.qos.logback:logback-classic:1.4.14`

### 테스트 의존성
- `org.testcontainers:testcontainers:1.19.3`
- `org.testcontainers:kafka:1.19.3`
- `org.testcontainers:junit-jupiter:1.19.3`
- `org.awaitility:awaitility:4.2.0`
- `org.assertj:assertj-core:3.27.6`
- `org.junit.jupiter:junit-jupiter:6.0.1`

---

## 새로운 Phase 추가하기

### 1. 패키지 생성

```bash
mkdir -p kafka/src/test/java/io/github/jongminchung/kafka/phase2
```

### 2. 테스트 클래스 작성

```java
package io.github.jongminchung.kafka.phase2;

import io.github.jongminchung.kafka.KafkaTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Phase 2: Producer 학습")
class KafkaProducerTest extends KafkaTestBase {

    @Test
    @DisplayName("2.1 동기 전송")
    void testSyncSend() {
        // 구현
    }
}
```

### 3. 테스트 실행

```bash
./gradlew :kafka:test --tests "io.github.jongminchung.kafka.phase2.*"
```

---

## 트러블슈팅

### TestContainers가 Docker 이미지를 다운로드하는 중
첫 실행 시 `confluentinc/cp-kafka:7.6.0` 이미지를 다운로드하므로 시간이 걸립니다.

```bash
# Docker 이미지 확인
docker images | grep kafka
```

### 포트 충돌
Docker Compose 사용 시 포트 충돌이 발생할 수 있습니다:

```bash
# 9092, 8080 포트 확인
lsof -i :9092
lsof -i :8080

# Kafka 클러스터 중지
./gradlew :kafka:dockerComposeDown
```

### 테스트 로그 확인

```bash
# 테스트 로그 출력 활성화
./gradlew :kafka:test --info

# 또는 build/reports/tests/test/index.html 확인
open kafka/build/reports/tests/test/index.html
```

---

## 다음 단계

1. ✅ Phase 1 테스트 완료 확인
2. Phase 2: Producer 학습 시작
   - 동기/비동기 전송
   - acks 설정
   - Batch 처리
3. `docs/kafka/ROADMAP.md` 참고

---

## 참고 자료

- [Kafka 학습 로드맵](./ROADMAP.md)
- [환경 설정 가이드](./SETUP.md)
- [kafka 모듈 README](./README.md)
