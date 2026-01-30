# OpenTelemetry

https://www.youtube.com/watch?v=4I1yoSgKUZo

## OTel을 도입하면 뭐가 좋을까?

- **표준화** 일관된 방식으로 Observability 데이터 수집
- **확장성** 분산 아키텍처에 확장 가능한 방식
- **다양한 언어 지원** Java, Go, Python, TypeScript등에서 사용 가능
- **오픈 소스 및 벤더 중립성** 다양한 백엔드(ES, InfluxDB, Datadog 등)와 통합 가능
- **커뮤니티 지원** CNCF 커뮤니티의 활발한 지원

## OTel 구성 요소

**Otel Specification**

OpenTelemetry API, SDK, 데이터, 정의와 통신에 대한 명세

**Otel Collector**

telemetry 데이터를 중앙에서 수집, 처리 후 백엔드로 내보냄

아래는 Otel Collector의 배포 방식에 따라 Agent Mode와 Gateway Mode로 구분됩니다.

- Agent Mode (DaemonSet): Sidecar or K8s DaemonSet
- Gateway Mode (Deployment): 중앙 집중화 (수평 확장)

**Otel Instrumentation**

아래는 애플리케이션에서 계측을 수행하는 방식에 따라 SDK와 Agent로 구분됩니다.

- SDK: 코드 기반 계측(Instrumentation)
- Agent: 자동 계측(Instrumentation)
