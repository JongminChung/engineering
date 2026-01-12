# AGENTS.md

## 구조

- Spring Boot 앱은 `study/api-communication/src/main/java/io/github/jongminchung/study/apicommunication`에 있습니다.
- REST 컨트롤러, gRPC 서비스, 보안, 메트릭, 레이트 리밋은 패키지별(`orders`, `grpc`, `security`, `metrics`)로 분리합니다.
- Protobuf 정의는 `study/api-communication/src/main/proto`에 둡니다.
- 테스트는 `study/api-communication/src/test/java`에 두고 REST/gRPC/서비스 로직을 커버합니다.

## 명령어

- 로컬 실행: `./gradlew :study:api-communication:bootRun`
- 모듈 테스트: `./gradlew :study:api-communication:test`
- 단일 테스트: `./gradlew :study:api-communication:test --tests "...OrderControllerTest"`
- 빌드: `./gradlew :study:api-communication:build`

## 코딩 규칙

- Spring 표준 네이밍(`*Controller`, `*Service`, `*Repository`, `*Config`)을 따릅니다.
- gRPC 코드는 `grpc`, REST 엔드포인트는 `orders/api`에 둡니다.
- 설정 프로퍼티는 `config` 패키지에 두고 `application.yml` 키와 매핑합니다.
- 포맷은 Spotless와 `.editorconfig`를 따릅니다.

## 테스트 규칙

- 테스트는 JUnit Jupiter와 `spring-boot-starter-test`를 사용합니다.
- REST 테스트는 필터/헤더/레이트 리밋 동작을 포함합니다.
- gRPC 테스트는 인프로세스 서버를 사용하고 인증 메타데이터/에러 매핑을 검증합니다.
- 네트워크 호출은 실제 호출 대신 모킹/인프로세스를 우선합니다.

## 보안/설정

- API 키는 `application.yml`에서 해시로 관리하며 로컬은 샘플 값을 사용합니다.
- 헤더/인증 흐름 변경 시 REST와 gRPC 경로 모두를 동기화합니다.
