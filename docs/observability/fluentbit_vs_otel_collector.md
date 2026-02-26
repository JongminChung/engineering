# OTel Collector vs. Fluent Bit

## 히스토리 및 진화 (History & Evolution)

### Fluent Bit의 변화 [^1]

과거 로그 수집기에 국한되었던 Fluent Bit은 v2.0을 기점으로 모든 텔레메트리 데이터(Logs, Metrics, Traces)를 처리하는 통합 에이전트로 진화했습니다.

- **v2.0:** 로그 중심에서 벗어나 메트릭과 트레이스 지원 시작.
- **v3.0:** 고성능 C기반 OTLP 처리 능력 강화.
- **v4.0 (2025~2026):** 조건부 프로세서(if/else), Zig 기반 고성능 플러그인, 그리고 정교한 트레이스 파서를 도입하여 사실상 OTel Collector의 에이전트 역할을 완벽히 대체하게 되었습니다.

---

## Fluent Bit v4 vs. OTel Collector 비교

| 비교 항목       | Fluent Bit v4 (Agent)               | OTel Collector (Gateway)                  |
| :-------------- | :---------------------------------- | :---------------------------------------- |
| **개발 언어**   | C / Zig (Ultra-lightweight)         | Go (Garbage Collected)                    |
| **리소스 점유** | 극히 낮음 (10~30MB RAM)             | 보통 (100~300MB+ RAM)                     |
| **처리 로직**   | Lua 스크립트, 단순 필터링           | OTTL (Transform Language), 복합 가공      |
| **데이터 모델** | 내부 포맷(C-Metrics/Logs) 변환      | OTLP 네이티브 (Native Signal 처리)        |
| **샘플링**      | Probabilistic, 제한적 Tail-sampling | 고급 Tail-based sampling (전역 상태 관리) |
| **주요 용도**   | 노드/에지 데이터 수집 및 전달       | 중앙 집중형 데이터 거버넌스 및 라우팅     |

---

## 데이터 거버넌스와 라우팅

사용자 환경에 따라 Fluent Bit 단독 구성과 OTel Collector 병행 구성을 선택할 수 있습니다.

### Fluent Bit 단독 사용이 유리한 경우

- **리소스 최적화:** Edge 디바이스나 리소스가 극도로 제한된 환경.
- **단순 파이프라인:** 수집처와 목적지(벤더)가 명확하고 변환 로직이 단순한 경우.
- **성능 중심:** 초당 수백만 라인의 로그를 최소 비용으로 처리해야 할 때.

### OTel Collector Gateway가 필요한 경우

- **중앙 집중식 제어:** 수천 개의 에이전트 설정을 개별적으로 관리하기 어려운 대규모 환경.
- **고급 데이터 변환:** OTTL을 사용하여 트레이스를 메트릭으로 변환하거나, 복잡한 비즈니스 로직에 따른 데이터 가공이 필요한 경우.
- **비용 거버넌스:** 특정 팀의 데이터는 저렴한 스토리지(S3)로, 장애 데이터는 고비용 벤더(Datadog 등)로 실시간 분기 처리.
- **보안:** 벤더 API Key 등 민감한 정보를 각 노드가 아닌 중앙 게이트웨이 한 곳에서만 관리.

---

## 권장 아키텍처: Hybrid (Two-Tier) 전략

대규모 기업 환경에서는 **Fluent Bit(Edge) + OTel Collector(Gateway)** 조합이 업계 표준(Best Practice)으로 자리 잡고 있습니다.

1.  **Tier 1: Edge (Fluent Bit v4)**
    - 모든 노드에 DaemonSet으로 배포.
    - OS 로그, 컨테이너 로그, 앱 메트릭을 OTLP로 수집.
    - 가벼운 필터링 후 중앙 Gateway로 신속히 전달.

2.  **Tier 2: Gateway (OTel Collector)**
    - 중앙 집중형 서비스로 배포.
    - 모든 에이전트로부터 데이터를 수신하여 표준화(Standardization).
    - 전역적인 Tail-sampling 수행 및 벤더별 맞춤형 라우팅.

## 결론

Fluent Bit v4는 기술적으로 OTel Collector의 많은 기능을 흡수했습니다. 하지만 **'기능의 가능 여부'**보다 **'운영의 확장성과 데이터 제어권'** 관점에서 접근해야 합니다.

단순한 환경이라면 Fluent Bit으로 통합하여 복잡도를 낮추고, 거버넌스와 정교한 비용 관리가 필요한 대규모 환경이라면 OTel Collector를 게이트웨이로 두는 하이브리드 전략을 권장합니다.

[^1]: https://fluentbit.io/announcements/v2.0.0/#:~:text=Agent%20Bit%20(packages)-,Logs%2C%20Metrics%2C%20and%20Traces,-Fluent%20Bit%20has
