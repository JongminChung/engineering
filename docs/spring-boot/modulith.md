# Spring Modulith

Spring Modulith는 도메인 중심 모듈 구조를 강제하면서 모듈 간 의존성과 이벤트 통신을 점검하게 해주는 라이브러리임.
모듈 구조를 문서화하고 모듈 경계를 테스트로 검증하게 해줌.

## v2에서 달라진 점

- `@Modulith` 애노테이션으로 모듈리식 앱 선언 가능해짐.
- 기존 `@Modulithic`도 사용 가능한 케이스가 있음. 실제 사용 가능 여부는 프로젝트 의존성 버전에 맞춰 확인해야 함.
- 코어/테스트 스타터가 분리되어 있어 `spring-modulith-starter-core`, `spring-modulith-starter-test` 조합으로 구성하게 됨.
- 이벤트 발행 레지스트리 설정이 `spring.modulith.events.*` 프로퍼티로 정리되어 있음.

## 기본 구성

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

@Modulith
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 모듈 구조 검증

```java
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithTests {
    private final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifyModules() {
        modules.verify();
    }
}
```

## 이벤트 발행 설정

```properties
spring.modulith.events.publication-registry.enabled=true
spring.modulith.events.retention-policy=P30D
spring.modulith.events.republish-outstanding-events-on-restart=true
```

## 문서화

- `spring-modulith-starter-test`의 `Documenter`로 모듈 다이어그램을 생성할 수 있음.
- 테스트에서 `ApplicationModules.of(Application.class)`로 검증 후 출력하면 됨.

## PoC 적용 위치

- 모듈 경로: `study/cloud`
- 모듈 구성: `users`, `auth`, `iam`
- `iam` 모듈에서 PDP/PEP을 분리해서 정책 결정을 명확히 하도록 구성함.

---

참조: [Spring Modulith Reference Documentation](https://docs.spring.io/spring-modulith/reference/)
