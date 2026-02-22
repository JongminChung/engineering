# Spring Modulith 정리

목적: Spring Modulith 공식 레퍼런스(Overview 제외)를 기준으로
"모듈 경계 정의, 노출, 검증, 운영"을 실무 관점에서 정리합니다.

참고:

- https://docs.spring.io/spring-modulith/reference/fundamentals.html
- https://docs.spring.io/spring-modulith/reference/verification.html
- https://docs.spring.io/spring-modulith/reference/events.html
- https://docs.spring.io/spring-modulith/reference/testing.html
- https://docs.spring.io/spring-modulith/reference/moments.html
- https://docs.spring.io/spring-modulith/reference/documentation.html
- https://docs.spring.io/spring-modulith/reference/runtime.html
- https://docs.spring.io/spring-modulith/reference/production-ready.html
- https://docs.spring.io/spring-modulith/reference/appendix.html

## 1) 모듈 정의와 노출(핵심)

기본 아이디어:

- 모듈 = "제공 API + 내부 구현 + 선택적 required interface"
- 애플리케이션 메인 패키지의 direct sub-package를 기본 모듈로 봅니다.
- 기본적으로 direct sub-package 루트 타입이 모듈 공개 API가 됩니다.
- 하위 패키지는 내부 구현으로 취급됩니다.

공식이 권장하는 공개 방식:

- 외부에 노출할 타입은 모듈 루트 패키지에 둡니다.
- 루트 밖 타입을 공개해야 하면 `@NamedInterface`로 named interface를 명시합니다.
- 모듈 간 의존은 `@ApplicationModule(allowedDependencies = …)`로 화이트리스트화할 수 있습니다.

고급 구성:

- Nested module: 상위 모듈 하위에 모듈을 중첩 정의.
- Open module: 경계가 느슨한 모듈(점진적 전환용).
- 모듈 감지 전략은 direct-subpackages / explicitly-annotated / custom으로 교체 가능합니다.

## 2) 구조 검증(Verification)

`ApplicationModules.of(…)`를 통해 모델을 만들고 `verify()` 또는 `detectViolations()`로 검사합니다.

대표 검증 항목:

- 사이클 없는 의존성
- API로만 접근(내부 구현 접근 금지)
- 허용 의존성(allowedDependencies) 위반 여부

jMolecules + ArchUnit 연계를 켜면 DDD 아키텍처 룰까지 확장 검증할 수 있습니다.

## 3) 모듈 통합 테스트

`@ApplicationModuleTest`가 중심입니다.

- `STANDALONE`: 대상 모듈만
- `DIRECT_DEPENDENCIES`: 직접 의존 모듈 포함
- `ALL_DEPENDENCIES`: 전체 의존 트리 포함

모듈 경계를 지키며 테스트하려면:

- 외부 모듈 빈은 모킹하거나 이벤트 경계로 대체
- Scenario API로 이벤트 기반 플로우를 선언적으로 테스트

## 4) 이벤트 모델

모듈 간 동기 호출 대신 이벤트 통신을 기본 전략으로 권장합니다.

- `@ApplicationModuleListener`: 모듈 경계 친화적 이벤트 리스너 구성
- Event Publication Registry: 비동기/트랜잭션 경계에서 전달 보장
- CompletedEventPublications API: 완료 이벤트 조회/재처리 지원
- Completion mode: update / delete / archive 선택 가능
- Externalized Events: 이벤트를 메시징/외부 시스템으로 내보내는 라우팅 모델 제공

## 5) Moments(시간 이벤트)

시간 경계 이벤트를 애플리케이션 이벤트로 발행합니다.

- HourHasPassed, DayHasPassed 등 제공
- TimeMachine으로 시간 흐름 테스트 가능
- 타임존/Clock 제어를 포함한 시간 기반 유스케이스 테스트에 유용

## 6) 문서화

ApplicationModules 모델에서 문서를 생성할 수 있습니다.

- 모듈 캔버스 생성
- PlantUML/C4 다이어그램 생성
- 출력 디렉터리 지정 가능

즉, 모듈 경계를 "코드 + 테스트 + 다이어그램"으로 동기화할 수 있습니다.

## 7) 런타임 지원

런타임에 모듈 초기화를 다루는 컴포넌트를 제공합니다.

- `ApplicationModuleInitializer`
- `ApplicationModuleListener`

모듈 순서/의존을 고려한 초기화, 실행 중 모듈 상태 점검 시나리오에 사용합니다.

## 8) 프로덕션 기능

Actuator 통합과 관측(Observability) 기능을 제공합니다.

- `modulith` Actuator endpoint
- Micrometer Observation/Tracing 통합
- 이벤트 외부화 및 레지스트리 운영 관련 스타터와 옵션 제공

## 9) Appendix 핵심

- 아티팩트/스타터 구성
- 이벤트 퍼블리케이션 레지스트리 저장소별 스키마와 운영 힌트
- 추가 기능 모듈(예: testing, moments, observability) 선택 가이드

## 10) Python/uv에 이식할 때의 포인트

Spring Modulith의 핵심을 Python으로 옮기면 아래 4가지입니다.

- 모듈 루트 패키지를 공개 API로 본다.
- 내부 패키지(`_internal`)는 비공개 계약으로 본다.
- 허용 의존성은 import-linter 규칙으로 화이트리스트화한다.
- 이벤트 기반 통신/시나리오 테스트로 모듈 결합을 낮춘다.

Python은 `exports` 같은 강제 필드가 약하므로,
"문서화 + import 규칙 테스트 + 공개 API 계약 테스트"를 함께 운영해야 합니다.
