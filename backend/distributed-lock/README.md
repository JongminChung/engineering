# Distributed Lock

Spring Boot 4 모듈 구조를 기준으로 분산락을 제공하는 라이브러리입니다.

## 모듈

- `distributed-lock-core`: 도메인/정책/키 규칙
- `distributed-lock-provider-redis`: Redisson 기반 구현
- `distributed-lock-provider-jdbc`: JDBC(DataSource + SQL) 기반 구현
- `distributed-lock-spring`: AOP/SpEL/LockTemplate
- `distributed-lock-spring-boot-autoconfigure`: 자동 설정
- `distributed-lock-spring-boot-starter`: 기본 Redis 스타터
- `distributed-lock-spring-boot-starter-redis`
- `distributed-lock-spring-boot-starter-jdbc`
- `distributed-lock-test`: 테스트 유틸
- `distributed-lock-dependencies`: BOM(선택)

## 설치 예시

```kotlin
dependencies {
    implementation(project(":distributed-lock:spring-boot-starter-redis"))
    // 또는 JDBC
    // implementation(project(":distributed-lock:spring-boot-starter-jdbc"))
}
```

## 설정

```yaml
distributed-lock:
  provider: redis # redis | jdbc
  wait-time: 5s
  lease-time: 30s
  fair: false
  redis:
    key-prefix: "lock:"
  jdbc:
    table-name: "distributed_locks"
```

## JDBC 스키마

```sql
create table if not exists distributed_locks (
  lock_key varchar(255) primary key,
  owner varchar(255) not null,
  expires_at timestamp(6) not null,
  locked_at timestamp(6) not null
);
```

## 사용 예시

### 어노테이션

```java
import io.github.jongminchung.distributedlock.spring.annotation.DistributedLock;

@DistributedLock(key = "#args[0]", waitTimeMs = 1000, leaseTimeMs = 5000)
public void updateOrder(String orderId) {
    // ...
}
```

### 프로그래밍 방식

```java
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.spring.support.LockTemplate;

LockRequest request = LockRequest.of(LockKey.of("order:1"));
lockTemplate.execute(request, () -> {
    // ...
});
```

## Redisson 설정 예시

```java
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RedissonConfig {
    @Bean
    RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }
}
```
