---
marp: true
theme: resume
size: A4D150
---

<!-- class: lead -->

# 정종민

*(Cloud & Infrastructure Engineer)*

|                                     |                                               |
|-------------------------------------|-----------------------------------------------|
| **Email**: <chungjm0711@gmail.com>    | **Linkedin**: [jongminchung][linkedin]        |
| **Blog**: [blog.octobug.site][blog] | **GitHub**: [github.com/jongminchung][github] |

<!-- markdownlint-disable MD012 -->
[github]: <https://github.com/jongminchung>

[linkedin]: <https://www.linkedin.com/in/jongminchung/>
<!-- markdownlint-disable MD012 -->

## About Me

클라우드 기반의 인프라 및 플랫폼 백엔드 개발자로서, 서비스 안정성과 운영 효율성을 극대화하는 아키텍처 설계와 자동화에 집중합니다. 단순히
기능을 구현하는 것을 넘어, 엔지니어링 프로세스 전반을 개선하여 팀의 생산성을 높이는 일에 가치를 둡니다.

### 핵심 역량

- **Cloud Native Infrastructure**: VPC, LB, Storage 등 클라우드 주요 상품군에 대한 깊은 이해와
	REST API 설계 및 운영 역량 보유.
- **Software Craftsmanship**: DDD, Clean Architecture, 포트와 어댑터 패턴 등을 활용한 유지보수성
	높은 코드 작성 지향.
- **DX & Automation**: CI/CD 파이프라인 표준화, Gradle 플러그인 개발 등을 통해 개발자 경험(DX)을 개선하고 반복
	업무 자동화.
- **Technical Sharing**: 복잡한 기술적 문제를 문서화하고 팀 내 공유하며 함께 성장하는 문화 주도.

## Professional Skills

- **Backend**: `Java`, `Spring Boot`, `Node.js (TypeScript)`, `Express`
- **Infrastructure**: `Docker`, `Kubernetes`, `VPC`, `Object Storage`,
	`Load Balancer`
- **Database**: `MySQL`, `Redis`, `Elasticsearch`
- **CI/CD & DevOps**: `GitLab CI/CD`, `GitHub Actions`,
	`Gradle Plugin Development`
- **Monitoring**: `ELK Stack`, `Grafana`, `Sentry`, `OpenTelemetry`

### Open Source Contributions

- **MinioAdminClient**: `listServiceAccount` API 필드 추가 및 정책 관리 메서드(
	`attachPolicies`, `detachPolicies`) 구현.
- **swagger-core**: Java Byte 타입을 OAS integer 데이터 타입으로 변환하는 로직 수정.
- **apache-seata**: `mvn spotless:apply` 실패 버그 수정.

## Experience

- **가비아 (Gabia)** `Cloud Development Team` | 사원
	*2023.01 ~ 현재*
 	- 클라우드 핵심 상품 API 설계/개발 및 레거시 아키텍처 고도화 주도.
 	- 전사 표준 CI/CD 컨벤션 수립 및 공통 개발 라이브러리 보급.

---

## Projects

### 클라우드 상품 콘솔 개발 및 운영

*2023.06 ~ 2025.04*

**기술 스택**: `Java`, `Spring Boot`, `MySQL`, `Redis`, `Docker`, `ELK`

- **11개 주요 클라우드 상품 REST API 설계 및 구현**: 서버, 스토리지, VPC, 네트워크 등 핵심 인프라 서비스 API 구축.
- **API 게이트웨이 도입**: 중앙 집중식 인증·인가 처리 및 Passport 패턴 적용으로 보안성 및 마이크로서비스 간 연동 효율 개선.
- **생산성 도구 개발**: OpenAPI Generator 기반 No-code API 생성 프로세스 구축 및 공통 JPA
	AutoConfigure 라이브러리 배포.

### 오브젝트 스토리지 서비스 고도화

*2025.07 ~ 2025.12*

**기술 스택**: `Java`, `Spring Boot`, `MySQL`, `Testcontainers`, `LocalStack`

- **아키텍처 개선**: DDD 및 Ports & Adapters 패턴을 적용하여 비즈니스 로직을 기술 스택으로부터 격리, 테스트 용이성 및
	유연성 확보.
- **성능 최적화**: `CompletableFuture` 기반의 비동기 병렬 호출로 복합 작업 응답 시간 단축 및 실패 시 자동
	Clean-up 로직 구현.
- **품질 보증**: Testcontainers와 LocalStack을 활용한 실제와 유사한 통합 테스트 자동화로 배포 안정성 강화.

### 공통 CI/CD 및 컨벤션 표준화

*2025.04 ~ 현재*

**기술 스택**: `GitLab CI/CD`, `Gradle Plugin`, `Static Analysis Tools`

- **CI/CD 파이프라인 통합**: 파편화된 스크립트를 공통 레포지토리로 통합하고, GitLab `include/extends` 기능을
	활용하여 신규 프로젝트 세팅 공수 **90% 절감**.
- **Gradle Convention Plugin 개발**: Spotless, ErrorProne, NullAway 등 정적 분석 설정을
	플러그인화하여 전사 프로젝트 품질 상향 평준화.
- **Spec-First 프로세스 구축**: OpenAPI 명세 기반의 Interface/DTO 자동 생성 및 Redocly를 활용한 명세
	검증 자동화.

### 로드 밸런서 세션 모니터링 시스템

*2023.06 ~ 2023.09*

- **메트릭 수집 자동화**: 에이전트(Metricbeat, Filebeat) 설정 자동 동기화 및 네트워크 단절 시 데이터 유실 방지를 위한
	재전송 로직 구현.
- **데이터 시각화 API**: Elasticsearch에 수집된 대규모 세션 데이터를 효율적으로 조회할 수 있는 콘솔 API 개발.

[blog]: <https://github.com/jongminchung/>


