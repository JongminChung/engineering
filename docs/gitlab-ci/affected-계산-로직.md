# GitLab CI Affected 계산 로직

## 목적

- MR에서 변경된 모듈과 그 **의존 모듈만** 테스트하여 빠른 피드백 제공
- 공통 모듈 변경 시에도 **영향받는 모듈만** 실행

## 입력

- base SHA: 대상 브랜치 기준 (예: `CI_MERGE_REQUEST_TARGET_BRANCH_SHA`)
- head SHA: 현재 커밋 (예: `CI_COMMIT_SHA`)
- 변경 파일 목록: `git diff --name-only $BASE...$HEAD`

## 핵심 개념

- **모듈 식별**: 변경 파일에서 상위 디렉터리를 역추적하여 가장 가까운 `build.gradle.kts` 또는 `build.gradle`를 찾는다.
- **영향 확장**: 변경된 모듈을 기준으로 **역방향 의존 관계**(dependents)를 찾아 포함한다.
- **공통 모듈**: 공통 모듈 자체가 “특별 규칙”이 아니라, **그 모듈을 의존하는 모듈만** 테스트한다.

## 알고리즘 (요약)

1) 변경 파일 목록 수집
2) 각 파일 → 모듈 경로 매핑
3) 변경 모듈 집합 생성
4) 의존 그래프를 기반으로 **의존 모듈(상위 모듈)**까지 확장
5) 최종 영향을 받는 모듈 리스트 출력

## 모듈 매핑 규칙

- `foo/bar/baz/X.java` → `foo/bar/baz/build.gradle.kts`까지 역추적
- `settings.gradle.kts`, `gradle.properties`, `build-logic/**` 변경 시
  - 모든 모듈에 영향이 크므로 **전체 모듈 실행**으로 처리 (예외 규칙)

## 의존 그래프 확보 방법

권장: Gradle Task로 프로젝트 의존 그래프를 JSON으로 출력하고 CI에서 사용

- `./gradlew -q printProjectGraph`
  - 출력 형식: `{ ":moduleA": [":moduleB", ":moduleC"], ... }`
- CI는 이 JSON을 이용해 **역방향 의존 모듈을 계산**한다.

대안: CI에서 `./gradlew :<module>:dependencies`를 파싱

- 정확하지만 느리고 출력이 불안정하여 권장하지 않음

## 산출물

- `AFFECTED_MODULES`: `:path` 형식의 모듈 리스트
  예: `:distributed-lock:core :distributed-lock:spring-boot-starter`

## 구현 요소

- Gradle 태스크: `printProjectGraph` (프로젝트 의존 그래프 JSON 출력)
- 스크립트: `scripts/affected-modules.js` (변경 모듈 + 의존 모듈 계산)

## GitLab CI 사용 예시

```bash
AFFECTED_MODULES=$(node scripts/affected-modules.js)
if [[ -z "${AFFECTED_MODULES}" ]]; then
  echo "No affected modules detected."
  exit 0
fi

for module in $AFFECTED_MODULES; do
  ./gradlew ${module}:test ${module}:integrationTest
done
```

## IntegrationTest 스모크/전체 분리 전략

### 개요
- MR에서는 **스모크 integrationTest**만 실행해 빠른 피드백 제공
- 기본 브랜치/스케줄에서는 **전체 integrationTest** 실행
- 스모크 대상은 `@Tag("smoke")`로 지정

### Gradle 설정
- `-PintegrationTestMode=smoke`를 전달하면 `integrationTest`가 `smoke` 태그만 실행
- 태그가 없는 테스트는 스모크에서 제외된다

### GitLab CI 예시

```yaml
test:affected:
  script:
    - AFFECTED_MODULES=$(node scripts/affected-modules.js)
    - for module in $AFFECTED_MODULES; do ./gradlew ${module}:test; done

integration:affected:smoke:
  script:
    - AFFECTED_MODULES=$(node scripts/affected-modules.js)
    - for module in $AFFECTED_MODULES; do ./gradlew -PintegrationTestMode=smoke ${module}:integrationTest; done

integration:affected:full:
  script:
    - AFFECTED_MODULES=$(node scripts/affected-modules.js)
    - for module in $AFFECTED_MODULES; do ./gradlew ${module}:integrationTest; done
```

### 사용 가이드
- MR에서 반드시 통과해야 하는 것은 `test` + `integrationTest(smk)`
- 전체 통합 테스트는 `main` 브랜치나 스케줄에서 수행
- 스모크 대상은 서비스 핵심 경로만 최소화해서 관리

## 의도한 동작 예시

- `distributed-lock:core` 변경
  → `distributed-lock:spring`, `distributed-lock:spring-boot-*` 등 **의존 모듈만** 테스트
- `study:infra` 변경
  → `study:infra`만 테스트
- `build-logic/**` 변경
  → **전체 모듈** 테스트 (예외 규칙)
