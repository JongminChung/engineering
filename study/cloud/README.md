# Cloud 학습 모듈

AWS 표준(de facto: AWS 공식 문서, RFC)을 기반으로 Cloud 관련 기능을 Modulith로 분리해 학습하는 PoC 앱임.
Kafka, PostgreSQL, IAM Policy, S3, STS, OIDC, OAuth2, IAM User(root, iam user)를
단계적으로 적용하고 테스트하는 것이 목표임.

## 핵심 목표

- 모듈 경계를 명확히 하여 기능 확장 시 영향 범위를 최소화함.
- AWS 표준과 RFC를 기준으로 인증/인가, 토큰, 리소스 권한 모델을 설계함.
- Testcontainers 및 통합 테스트로 학습 결과를 검증함.

## 모듈 구상(예정)

- `iam`: 정책 결정(PDP), 정책 집행(PEP), 사용자/역할 모델
- `auth`: OAuth2, OIDC 인증 흐름, 토큰 검증
- `storage`: S3 버킷/오브젝트 권한 모델
- `identity`: STS, AssumeRole, 임시 자격 증명
- `infra`: Kafka, PostgreSQL, 메시징/데이터 영속

## 학습 범위

- **Modulith**: 모듈 경계 검증, 모듈 간 이벤트 및 문서화
- **Kafka**: 이벤트 발행/구독, 표준 메시징 패턴
- **PostgreSQL**: 영속 계층 및 마이그레이션 전략
- **IAM Policy**: PDP/PEP 분리, 최소 권한 원칙 적용
- **S3**: 버킷 정책, 객체 권한 제어
- **STS**: AssumeRole, 임시 자격 증명 흐름
- **OIDC**: ID 토큰 검증, 표준 클레임 처리
- **OAuth2**: Authorization Code, Client Credentials 플로우
- **IAM User**: root 사용자와 일반 IAM 사용자 권한 분리

## 표준 준수 원칙

- AWS 공식 문서 기반의 정책/흐름 적용함.
- RFC 명세를 기준으로 토큰 구조 및 클레임 해석함.
- 비표준 동작은 명시적으로 문서화하고 테스트에 반영함.

## 실행 및 테스트

```bash
./gradlew :study:cloud:test
```

## 참고

- AWS 공식 문서
- 관련 RFC (OAuth2, OIDC, JWT 등)
