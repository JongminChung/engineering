# Gitlab CI/CD Catalog and Structured Inputs

Gitlab 17부터 CI/CD Catalog GA와 Structured Inputs 기반의 Modular pipelines가 정식 도입되었다.

## Gitlab 17+ 변화 요약 (미래 기준점)

**Gitlab 17+ CI/CD의 핵심 변화**

- include: component 기반 재사용
- inputs: 로 구조화된 입력 전달
- CI/CD Catalog를 통한 중앙 탐색/배포
- 컴포넌트 단위 semver 버전 관리

```yaml
include:
    - component: gitlab.example.com/ci/build@1.2.0
      inputs:
          image: docker:26
          push: true
```

## GitLab 18 관점에서 본 "좋은 CI/CD 설계"

GitLab 18이 요구하는 CI/CD 설계 기준은 다음과 같다.

1. 컴포넌트는 하나의 책임만 가진다.
2. 입력은 명시적으로 정의한다.
3. 전역 변수 의존을 최소화한다.
4. 버전 없는 include는 금지한다.
5. 조합은 소비자, 구현은 제공자 책

Structured inputs

- CI/CD 컴포넌트의 “공식 인터페이스”

Modular pipelines

- 파이프라인을 컴포넌트 조합으로 바라보는 설계 방식

---
