# 최신 클라우드 네이티브 스택 추천

## Backend · DevOps · Frontend를 아우르는 표준 아키텍처 가이드

클라우드 네이티브 환경에서 “무엇을 쓰는가”보다 중요한 것은
**어디까지를 표준으로 고정하고, 어디부터를 선택 사항으로 둘 것인가**다.

이 문서는 **CNCF 성숙도, 운영 안정성, 인력 수급, 벤더 락인 최소화**를 기준으로
2025년 현재 가장 현실적인 **클라우드 네이티브 표준 스택**을 정리한다.

---

## 1. 아키텍처 설계 원칙 (요약)

- **Kubernetes + GitOps + OpenTelemetry**는 고정 코어
- 네트워크/트래픽 계층은 **Gateway API 중심**
- 언어·프레임워크는 자유, **운영 규격은 통일**
- 서비스 메시·고급 L7 기능은 *필요할 때만*
- “연결 / 재시도 / 관측”은 처음부터 설계

---

## 2. 플랫폼 & 인프라 코어

| 구분           | 역할           | 권장 스택                        | 선택 이유                 | 주의사항            |
|--------------|--------------|------------------------------|-----------------------|-----------------|
| 컨테이너 오케스트레이션 | 워크로드 관리      | Kubernetes (EKS / GKE / AKS) | CNCF 표준, 생태계 최대       | 분기별 업그레이드 정책 필수 |
| 컨테이너 런타임     | 컨테이너 실행      | containerd                   | 사실상 표준                | 직접 튜닝 불필요       |
| **CNI**      | **Pod 네트워킹** | **Calico**                   | 네트워크 정책 성숙, 대규모 운영 검증 | 정책 복잡도 관리       |
| 스토리지         | 영속 볼륨        | CSI + 클라우드 스토리지              | 확장성, 관리 용이            | 성능/비용 프로파일 확인   |

> **Calico 선택 이유**
> - L3/L4 NetworkPolicy 성숙도 높음
> - 서비스 메시 없이도 **강력한 네트워크 격리** 가능

---

## 3. 배포 & 운영 (DevOps / GitOps)

| 구분     | 역할     | 권장 스택                      | 선택 이유                 | 주의사항        |
|--------|--------|----------------------------|-----------------------|-------------|
| GitOps | 선언적 배포 | Argo CD                    | CNCF Graduated, 감사 추적 | Flux와 혼용 금지 |
| 패키징    | 앱 배포   | Helm + Kustomize           | 유연한 배포 전략             | 템플릿 난립 방지   |
| 배포 전략  | 점진 배포  | Argo Rollouts (선택)         | Canary/Blue-Green     | 모든 서비스 강제 ❌ |
| CI     | 빌드/테스트 | GitHub Actions / GitLab CI | SaaS 관리 비용 ↓          | 파이프라인 표준화   |

---

## 4. Observability (관측성)

| 구분      | 역할    | 권장 스택              | 선택 이유      | 주의사항              |
|---------|-------|--------------------|------------|-------------------|
| 계측 표준   | 수집 규격 | OpenTelemetry      | 벤더 중립, 표준  | 계측 규칙 통일 필수       |
| Metrics | 메트릭   | Prometheus / Mimir | CNCF 표준    | cardinality 관리    |
| Logs    | 로그    | Loki / Elastic     | 비용 효율 / 검색 | 로그 폭증 주의          |
| Traces  | 분산 추적 | Tempo / Jaeger     | OTel 친화    | trace sampling 설계 |

---

## 5. 보안 & 정책 (Shift-left)

| 구분      | 역할                | 권장 스택                  | 선택 이유        | 주의사항         |
|---------|-------------------|------------------------|--------------|--------------|
| 이미지 스캔  | 취약점               | Trivy                  | 경량, CI 통합 용이 | 결과 정책화 필수    |
| 서명      | Supply Chain      | Cosign (Sigstore)      | 무서명 배포 차단    | 정책과 함께 사용    |
| 정책      | Admission Control | Kyverno 또는 Gatekeeper  | 정책 as 코드     | 과도한 정책 ❌     |
| Secrets | 비밀 관리             | KMS + External Secrets | 운영 단순        | Secret 남용 금지 |

---

## 6. 트래픽 & API 계층 (Gateway API 중심)

> **Ingress는 더 이상 표준의 중심이 아니다.**
> Kubernetes의 공식 방향은 **Gateway API**다.

| 구분                 | 역할               | 권장 스택                         | 선택 이유             | 주의사항               |
|--------------------|------------------|-------------------------------|-------------------|--------------------|
| **Gateway API**    | **L4/L7 트래픽 표준** | **Gateway API (v1.x)**        | 역할 분리, 표준화        | Controller 선택 중요   |
| Gateway Controller | 구현체              | Envoy Gateway / Istio Gateway | CNCF 중심 생태계       | 단일 컨트롤러 유지         |
| API Gateway        | 인증·인가            | 전용 Gateway (선택)               | OAuth, Rate limit | Gateway API와 역할 분리 |
| WAF                | L7 보안            | 클라우드 WAF                      | 보안 전문화            | 중복 정책 방지           |

### Gateway API를 선택하는 이유

- Ingress 대비 **역할 명확화**
  - GatewayClass → 인프라 팀
  - Gateway → 플랫폼 팀
  - HTTPRoute → 서비스 팀
- L4/L7 확장성 (TCP, UDP, HTTP)
- 벤더 중립 API + 구현체 교체 가능

---

## 7. 백엔드 애플리케이션 스택

| 구분    | 역할        | 권장 스택                    | 선택 이유      | 주의사항          |
|-------|-----------|--------------------------|------------|---------------|
| 주요 언어 | API 서버    | Java/Kotlin, Go, Node.js | 인력 수급, 생태계 | 언어 혼합 시 규격 통일 |
| 프레임워크 | 웹/API     | Spring Boot, Gin, NestJS | 운영 검증      | 과도한 추상화 ❌     |
| 데이터   | RDB       | PostgreSQL / MySQL       | 표준 DB      | 매니지드 권장       |
| 캐시    | In-memory | Redis                    | 성능 향상      | TTL 설계 필수     |
| 이벤트   | 메시징       | Kafka / NATS             | 비동기 처리     | 용도 구분 명확화     |

---

## 8. 프론트엔드(Frontend) 스택

| 구분         | 역할  | 권장 스택              | 선택 이유         | 주의사항        |
|------------|-----|--------------------|---------------|-------------|
| 프레임워크      | UI  | React + TypeScript | 사실상 표준        | 상태 관리 단순화   |
| SSR / Edge | 성능  | Next.js            | SEO / Edge 대응 | 필요 없는 SSR ❌ |
| 빌드         | 번들  | Vite               | 빠른 빌드         | 설정 분산 주의    |
| 배포         | 전달  | CDN + Edge Cache   | 글로벌 성능        | 캐시 전략 필수    |
| FE 관측      | RUM | 오류·Trace 연동        | 사용자 가시성       | trace-id 연계 |

---

## 9. 한 장 요약 (최종 표준 스택)

| 영역            | 표준 선택                    |
|---------------|--------------------------|
| Orchestration | Kubernetes               |
| CNI           | **Calico**               |
| Traffic API   | **Gateway API**          |
| GitOps        | Argo CD                  |
| Observability | OpenTelemetry            |
| Security      | Policy as Code + Signing |
| Backend       | 언어 자유, 운영 규격 통일          |
| Frontend      | React + TypeScript       |

---

## 10. 결론

2025년의 클라우드 네이티브는
**Ingress 중심 아키텍처에서 Gateway API 중심 아키텍처로 이동**하고 있다.

- 트래픽은 명확한 책임으로 분리하고
- 네트워크는 정책 기반으로 통제하며
- 배포와 관측은 표준으로 고정한다

> **최신 스택이란
> 가장 많이 바뀌는 기술이 아니라
> 가장 오래 버틸 수 있는 구조다.**
