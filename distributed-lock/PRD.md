# 분산락 시스템

AOP 기반(어노테이션) + 프로그래밍 API(LockTemplate) 동시 제공 설계

- lock-provider-redis
- lock-provider-jdbc
- lock-provider-etcd

... 확장 가능한 구조로 설계 (현재는 Redis, JDBC 만 제공)

spring boot v4 와 같이 모듈을 분리하여 사용할 수 있도록 설계

## 요구사항

- 코어는 pure
- 구현체 통합은 어댑터
- autoconfigure 는 별도
- test util 별도

### 설계

- core
  - 분산락의 도메인/추상화(인터페이스), 공통 예외, 정책, 키 생성 규칙 등
  - 외부 의존성 최소화(가능하면 Spring 의존성도 최소화)
  - 원칙: 코어는 “기술(redis/db)” 모름. “스프링”도 최소화.

```text

- api
 - DistributedLock (핵심 인터페이스)
 - LockHandle (락 획득 결과/해제 토큰)
 - LockRequest (key, waitTime, leaseTime, fairness 등)
- policy
 - RetryPolicy, BackoffPolicy
 - LeasePolicy(watchdog 여부 등)
- key
 - LockKeyStrategy / LockKey
- exception
 - LockAcquisitionException
 - LockTimeoutException
 - LockReleaseException
```

- provider
  - 분산락의 구현체
  - 외부 의존성 최소화
  - 각 모듈은 해당 기술 스택 의존성을 가진다.

  - ```text
    - config (provider 내부 설정 모델; 가능하면 spring-boot props와 분리)
    - redisson (Redisson 기반 구현)
      - RedissonDistributedLock
    - support
    - RedisKeyPrefix, Serialization 등
    ```

- spring
  - Spring 통합(어노테이션, AOP, SpEL 키 평가, 트랜잭션 연계 포인트 등)

  - ```text
    - annotation
        - @WithDistributedLock (혹은 @DistributedLock)
    - aop
        - DistributedLockAspect
        - LockInvocation / LockOperation
    - expression
        - LockKeyExpressionEvaluator (SpEL)
    - resolver
        - LockKeyResolver (메서드/파라미터 기반)
    - support
        - LockTemplate (프로그래밍 방식 API)
        - LockingExecutor
     ```

- spring-boot-autoconfigure
  - @AutoConfiguration 기반 자동 설정
  - @ConditionalOnClass, @ConditionalOnProperty, @ConfigurationProperties 등
  - Boot 4의 오토컨피그 패턴을 명확히 분리

  - ```text
    - properties
       - DistributedLockProperties
    - condition
        - OnRedisClientCondition 등(필요 시)
    - configuration
        - DistributedLockAutoConfiguration
        - RedisLockAutoConfiguration
        - JdbcLockAutoConfiguration
    - customizer
        - LockCustomizer(사용자가 확장하는 포인트)
     ```

- spring-boot-starter
  - spring-boot-autoconfigure + 기본 provider 선택 (redis)
  - 목적: BOM/의존성 편의 제공

- test
  - 테스트 유틸(예: in-memory lock provider, Testcontainers 지원, 공통 테스트 픽스처)
  - Boot 4의 *-test 분리와 동일한 목적
  - 원칙: production 코드 의존성 그래프에 절대 섞이지 않도록 분리.

  - ```text
     - fake
       - InMemoryDistributedLock
     - fixture
       - LockTestFixture
     - containers
       - RedisContainerSupport (Testcontainers)
     - assertion
      - LockAssertions
     ```

## 의견 및 보완 제안

- core의 “기술/스프링 무관” 원칙을 유지하되 api/policy/key/exception은 core 내부 패키지로 고정해 모듈 수를
  줄이는 편이 관리에 유리합니다.
- provider 내부 config 모델은 유지하되, spring-boot `@ConfigurationProperties`와는 완전히 분리해야
  순수성이 보장됩니다.
- LockTemplate은 스프링 의존성이므로 `spring` 모듈에 두고 core로 이동하지 않습니다.
- autoconfigure는 조건/프로퍼티/커스터마이저를 분리해 유지보수 포인트를 명확히 합니다.
- test 모듈은 production 그래프와 완전히 분리하고, Testcontainers 지원도 test 모듈에 고정합니다.

## spring-boot-v4 스타일 모듈 구성 제안

### 모듈 구성

- distributed-lock-core
- distributed-lock-provider-redis
- distributed-lock-provider-jdbc
- distributed-lock-spring
- distributed-lock-spring-boot-autoconfigure
- distributed-lock-spring-boot-starter (기본 provider: redis)
- distributed-lock-spring-boot-starter-redis (선택)
- distributed-lock-spring-boot-starter-jdbc (선택)
- distributed-lock-test
- distributed-lock-dependencies (선택: BOM/플랫폼)

### 의존성 방향(핵심 규칙)

- distributed-lock-core ← 최하단(어떠한 스프링 의존성도 없음)
- distributed-lock-provider-* → distributed-lock-core
- distributed-lock-spring → distributed-lock-core (+ Spring AOP/Expression)
- distributed-lock-spring-boot-autoconfigure → distributed-lock-spring,
  distributed-lock-provider-*
- distributed-lock-spring-boot-starter* →
  distributed-lock-spring-boot-autoconfigure (+ 기본 provider)
- distributed-lock-test → distributed-lock-core (+ Testcontainers 등), 필요 시
  distributed-lock-provider-* 선택 의존

### 추가 제안

- starter를 하나로 유지한다면 기본 provider(redis)를 명시하고, 다른 provider는 별도 스타터로 분리하면 선택/충돌
  관리가 쉬워집니다.
- distributed-lock-dependencies(BOM/플랫폼)를 두면 버전 정합성과 스타터/프로바이더 조합 관리가 수월합니다.
- core에는 정책 인터페이스만 두고, 구현은 spring 또는 provider로 분리하는 편이 확장에 유리합니다.
