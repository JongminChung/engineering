# PRD: 모노레포 기반 GitLab CI/CD 공통 파이프라인 시스템

## 1. 개요

### 1.1 목적

멀티 팀 모노레포 환경에서 재사용 가능한 GitLab CI/CD 파이프라인 아키텍처를 설계하여,
중복 코드를 최소화하고 일관된 빌드/배포 프로세스를 제공한다.

### 1.2 배경

- 여러 팀(클라우드 자원 관리팀, 인증/인가팀 등)이 각자 독립된 GitLab 프로젝트로 모노레포 구성
- 각 팀의 모노레포는 여러 서브 프로젝트를 관리 (api, consumer, batch, admin-api 등)
- 변경된 경로에 따라 선택적으로 CI/CD 파이프라인 실행 필요
- 공통 작업(test, lint/format, 이미지 빌드, 배포)의 중복 정의 문제
- MR 단계에서 테스트 커버리지 리포트 등 아티팩트 공유 필요
- 환경별(MR/개발/운영) 배포 전략 분기 필요

### 1.3 목표

- 변경 감지 기반 선택적 Job 실행
- 공통 CI/CD Job의 재사용성 극대화
- 팀/프로젝트별 커스터마이징 가능한 확장 구조
- 유지보수성 및 가독성 향상

## 2. 요구사항

### 2.1 기능 요구사항

#### 2.1.1 공통 Job 템플릿

각 서브 프로젝트에서 공통으로 사용할 Job 정의:

- **Test Job**: 단위 테스트 및 통합 테스트 실행
- **Lint & Format Job**: 코드 품질 검사 및 포맷팅 검증
- **Build & Package Job**: JAR 빌드 및 Docker 이미지 생성
- **Deploy Job**: 개발 서버 배포 (선택적으로 SDK 배포 지원)

#### 2.1.2 경로 기반 변경 감지

- `.gitlab-ci.yml`의 `rules:changes` 기반 Job 트리거
- 프로젝트별 경로 패턴 정의 (예: `cloud-api/*`, `auth-api/*`, `cloud-batch/*`)
- 공통 라이브러리 변경 시 관련 모든 프로젝트 Job 실행

#### 2.1.3 환경별 파이프라인 분기

- **Merge Request**: 테스트 실행 및 커버리지 리포트 생성, 코드 품질 검사
- **개발 환경 배포**: main/develop 브랜치 push 시 자동 배포
- **운영 환경 배포**: Git Tag 기반 배포 (`CI_COMMIT_TAG` 활용)

#### 2.1.4 아티팩트 관리

- 테스트 커버리지 HTML 리포트를 MR에 자동 첨부
- JUnit 테스트 결과를 GitLab에 업로드하여 UI에서 확인
- Docker 이미지 빌드 정보 및 배포 로그 아티팩트로 보관

#### 2.1.5 확장 가능한 배포 전략

- 기본: 개발 서버 배포
- 확장: SDK/라이브러리 배포 (Maven Central, 내부 Artifact Repository)
- 환경별 배포 전략 분리 (dev, staging, production)

#### 2.1.6 공통 CI/CD 프로젝트 제공

별도의 GitLab 프로젝트에서 공통 파이프라인 템플릿 관리 및 배포

### 2.2 비기능 요구사항

- **성능**: 변경되지 않은 프로젝트는 Job 실행 생략하여 빌드 시간 최소화
- **유지보수성**: 공통 로직 변경 시 단일 소스에서 관리
- **가독성**: 각 팀이 자신의 `.gitlab-ci.yml`에서 명확히 설정을 파악 가능
- **확장성**: 새로운 팀/프로젝트 추가 시 최소한의 설정으로 적용

## 3. 아키텍처 설계

### 3.1 전체 구조

```
GitLab Organization
│
├── [공통 CI/CD 프로젝트] gitlab-cicd-templates
│   ├── templates/
│   │   ├── .test-template.yml
│   │   ├── .lint-template.yml
│   │   ├── .build-image-template.yml
│   │   ├── .deploy-dev-template.yml
│   │   ├── .deploy-prod-template.yml
│   │   └── .deploy-sdk-template.yml
│   ├── scripts/
│   │   ├── run-tests.sh
│   │   ├── run-lint.sh
│   │   ├── build-docker.sh
│   │   └── deploy.sh
│   └── README.md
│
├── [클라우드팀 모노레포] cloud-team-monorepo
│   ├── .gitlab-ci.yml
│   ├── cloud-api/
│   ├── cloud-consumer/
│   ├── cloud-batch/
│   └── common-libs/
│
└── [인증팀 모노레포] auth-team-monorepo
    ├── .gitlab-ci.yml
    ├── auth-api/
    ├── auth-admin-api/
    ├── auth-sdk/
    └── common-libs/
```

### 3.2 공통 CI/CD 프로젝트 (gitlab-cicd-templates)

#### 3.2.1 역할

- GitLab CI Job 템플릿 정의 (`.yml` 파일)
- 공통 빌드/배포 스크립트 제공
- 복잡한 로직을 스크립트로 추상화 (Shell 또는 CLI SDK)
- 버전 관리 및 변경 이력 추적

#### 3.2.2 프로젝트 구조

```
gitlab-cicd-templates/
├── templates/
│   ├── .test-template.yml
│   ├── .lint-template.yml
│   ├── .build-image-template.yml
│   ├── .deploy-dev-template.yml
│   ├── .deploy-prod-template.yml
│   └── .deploy-sdk-template.yml
├── scripts/
│   ├── test/
│   │   ├── run-test.sh
│   │   └── generate-coverage-report.sh
│   ├── build/
│   │   ├── build-jar.sh
│   │   ├── build-docker-image.sh
│   │   └── push-to-registry.sh
│   ├── deploy/
│   │   ├── deploy-k8s.sh
│   │   ├── rollback.sh
│   │   └── health-check.sh
│   └── common/
│       ├── notify-slack.sh
│       └── validate-environment.sh
├── cli/  (고도화 단계)
│   ├── package.json
│   ├── tsconfig.json
│   ├── src/
│   │   ├── commands/
│   │   │   ├── test.ts
│   │   │   ├── build.ts
│   │   │   └── deploy.ts
│   │   ├── utils/
│   │   │   ├── gradle.ts
│   │   │   ├── docker.ts
│   │   │   └── kubernetes.ts
│   │   └── index.ts
│   └── bin/
│       └── cicd-cli
├── README.md
└── CHANGELOG.md
```

#### 3.2.3 템플릿 구조

#### 3.2.4 스크립트 활용 전략

템플릿(`.yml`)은 **선언적 구조**를, 스크립트는 **복잡한 로직**을 담당합니다.

**스크립트가 필요한 경우:**

- 조건부 분기가 복잡한 경우 (환경 변수에 따른 다른 동작)
- 여러 단계를 순차적으로 실행해야 하는 경우
- 에러 핸들링이 필요한 경우
- 외부 API 호출 (Slack 알림, Jira 티켓 생성 등)
- 동적으로 설정 파일 생성이 필요한 경우

**템플릿에서 스크립트 호출 방법:**

```yaml
# gitlab-cicd-templates 프로젝트를 CI_PROJECT_DIR에 clone
.setup-scripts:
  before_script:
    - apk add --no-cache git
    - git clone --depth 1 --branch ${CICD_TEMPLATE_VERSION:-main} https://gitlab.com/org/gitlab-cicd-templates.git /tmp/cicd-scripts
    - chmod +x /tmp/cicd-scripts/scripts/**/*.sh
```

**`.test-template.yml`** (스크립트 활용)

```yaml
.test:
  stage: test
  image: gradle:8-jdk17
  before_script:
    - apk add --no-cache git bash curl
    - git clone --depth 1 --branch ${CICD_TEMPLATE_VERSION:-main} https://gitlab.com/org/gitlab-cicd-templates.git /tmp/cicd-scripts
    - chmod +x /tmp/cicd-scripts/scripts/**/*.sh
  script:
    - /tmp/cicd-scripts/scripts/test/run-test.sh $PROJECT_PATH
    - /tmp/cicd-scripts/scripts/test/generate-coverage-report.sh $PROJECT_PATH
  after_script:
    - /tmp/cicd-scripts/scripts/common/notify-slack.sh "Test completed for $PROJECT_PATH"
  artifacts:
    reports:
      junit: $PROJECT_PATH/build/test-results/test/*.xml
      coverage_report:
        coverage_format: cobertura
        path: $PROJECT_PATH/build/reports/jacoco/test/cobertura.xml
    paths:
      - $PROJECT_PATH/build/reports/jacoco/test/html/
    expose_as: 'Test Coverage Report'
  coverage: '/Total.*?([0-9]{1,3})%/'
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
      changes:
        - $PROJECT_PATH/**/*
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
      changes:
        - $PROJECT_PATH/**/*
```

#### 3.2.5 스크립트 구체적 예시

**`scripts/test/run-test.sh`**

```bash
#!/bin/bash
set -e

PROJECT_PATH=$1
COVERAGE_THRESHOLD=${COVERAGE_THRESHOLD:-80}

echo "Running tests for $PROJECT_PATH..."
cd "$PROJECT_PATH"

# Gradle 테스트 실행
if gradle test jacocoTestReport; then
  echo "✅ Tests passed"
else
  echo "❌ Tests failed"
  exit 1
fi

# 커버리지 체크
COVERAGE=$(grep -oP 'Total.*?\K[0-9]+' build/reports/jacoco/test/html/index.html | head -1)
if [ "$COVERAGE" -lt "$COVERAGE_THRESHOLD" ]; then
  echo "⚠️  Coverage $COVERAGE% is below threshold $COVERAGE_THRESHOLD%"
  exit 1
fi

echo "✅ Coverage $COVERAGE% meets threshold"
```

**`scripts/build/build-docker-image.sh`**

```bash
#!/bin/bash
set -e

PROJECT_PATH=$1
IMAGE_NAME=$2
IMAGE_TAG=${3:-$CI_COMMIT_SHA}

echo "Building Docker image for $PROJECT_PATH..."
cd "$PROJECT_PATH"

# Gradle 빌드
gradle bootJar

# Docker 이미지 빌드
docker build \
  --build-arg BUILD_DATE="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --build-arg VCS_REF="$CI_COMMIT_SHA" \
  --build-arg VERSION="$IMAGE_TAG" \
  -t "$IMAGE_NAME:$IMAGE_TAG" \
  -t "$IMAGE_NAME:latest" \
  .

# 이미지 정보 저장 (dotenv artifact)
echo "IMAGE_TAG=$IMAGE_TAG" >> build.env
echo "IMAGE_DIGEST=$(docker inspect --format='{{.Id}}' $IMAGE_NAME:$IMAGE_TAG)" >> build.env
echo "BUILD_TIME=$(date -u +%Y%m%d-%H%M%S)" >> build.env
```

**`scripts/deploy/deploy-k8s.sh`**

```bash
#!/bin/bash
set -e

DEPLOYMENT_NAME=$1
IMAGE_NAME=$2
IMAGE_TAG=$3
NAMESPACE=$4
CLUSTER_CONTEXT=$5

echo "Deploying $DEPLOYMENT_NAME to $NAMESPACE..."

# Kubernetes context 전환
kubectl config use-context "$CLUSTER_CONTEXT"

# 배포 전 현재 상태 확인
CURRENT_IMAGE=$(kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.template.spec.containers[0].image}')
echo "Current image: $CURRENT_IMAGE"
echo "New image: $IMAGE_NAME:$IMAGE_TAG"

# 이미지 업데이트
kubectl set image deployment/"$DEPLOYMENT_NAME" app="$IMAGE_NAME:$IMAGE_TAG" -n "$NAMESPACE"

# Rollout 상태 확인 (5분 타임아웃)
if kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout=5m; then
  echo "✅ Deployment successful"

  # Health check
  /tmp/cicd-scripts/scripts/deploy/health-check.sh "$DEPLOYMENT_NAME" "$NAMESPACE"
else
  echo "❌ Deployment failed, rolling back..."
  kubectl rollout undo deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"
  exit 1
fi
```

**`scripts/common/notify-slack.sh`**

```bash
#!/bin/bash

MESSAGE=$1
SLACK_WEBHOOK_URL=${SLACK_WEBHOOK_URL:-""}

if [ -z "$SLACK_WEBHOOK_URL" ]; then
  echo "SLACK_WEBHOOK_URL not set, skipping notification"
  exit 0
fi

curl -X POST "$SLACK_WEBHOOK_URL" \
  -H 'Content-Type: application/json' \
  -d "{
    \"text\": \"$MESSAGE\",
    \"attachments\": [{
      \"color\": \"good\",
      \"fields\": [
        {\"title\": \"Project\", \"value\": \"$CI_PROJECT_NAME\", \"short\": true},
        {\"title\": \"Branch\", \"value\": \"$CI_COMMIT_BRANCH\", \"short\": true},
        {\"title\": \"Commit\", \"value\": \"$CI_COMMIT_SHORT_SHA\", \"short\": true},
        {\"title\": \"Author\", \"value\": \"$GITLAB_USER_NAME\", \"short\": true}
      ]
    }]
  }"
```

**`.build-image-template.yml`** (스크립트 활용)

```yaml
.build-image:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - apk add --no-cache git bash
    - git clone --depth 1 --branch ${CICD_TEMPLATE_VERSION:-main} https://gitlab.com/org/gitlab-cicd-templates.git /tmp/cicd-scripts
    - chmod +x /tmp/cicd-scripts/scripts/**/*.sh
  script:
    - /tmp/cicd-scripts/scripts/build/build-docker-image.sh $PROJECT_PATH $IMAGE_NAME $CI_COMMIT_SHA
    - docker push $IMAGE_NAME:$CI_COMMIT_SHA
    - docker push $IMAGE_NAME:latest
  artifacts:
    reports:
      dotenv: build.env
  rules:
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
      changes:
        - $PROJECT_PATH/**/*
    - if: '$CI_COMMIT_TAG'
      changes:
        - $PROJECT_PATH/**/*
```

**`.deploy-dev-template.yml`** (스크립트 활용)

```yaml
.deploy-dev:
  stage: deploy
  image: bitnami/kubectl:latest
  before_script:
    - apk add --no-cache git bash curl
    - git clone --depth 1 --branch ${CICD_TEMPLATE_VERSION:-main} https://gitlab.com/org/gitlab-cicd-templates.git /tmp/cicd-scripts
    - chmod +x /tmp/cicd-scripts/scripts/**/*.sh
  environment:
    name: development/$PROJECT_NAME
    url: https://$PROJECT_NAME-dev.example.com
  script:
    - /tmp/cicd-scripts/scripts/deploy/deploy-k8s.sh $DEPLOYMENT_NAME $IMAGE_NAME $CI_COMMIT_SHA dev dev-cluster
  after_script:
    - /tmp/cicd-scripts/scripts/common/notify-slack.sh "✅ Deployed $PROJECT_NAME to development"
  rules:
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
      changes:
        - $PROJECT_PATH/**/*
```

#### 3.2.3 사용 방식

각 팀 모노레포의 `.gitlab-ci.yml`에서 `include`로 참조:

```yaml
include:
  - project: 'cicd/gitlab-cicd-templates'
    ref: main
    file:
      - '/templates/.test-template.yml'
      - '/templates/.lint-template.yml'
      - '/templates/.build-image-template.yml'
      - '/templates/.deploy-dev-template.yml'
      - '/templates/.deploy-prod-template.yml'

stages:
  - test
  - build
  - deploy

variables:
  REGISTRY: registry.gitlab.com/org/cloud-team-monorepo

# Cloud API 프로젝트
test:cloud-api:
  extends: .test
  variables:
    PROJECT_PATH: cloud-api

lint:cloud-api:
  extends: .lint
  variables:
    PROJECT_PATH: cloud-api

build:cloud-api:
  extends: .build-image
  variables:
    PROJECT_PATH: cloud-api
    IMAGE_NAME: $REGISTRY/cloud-api
  needs:
    - test:cloud-api

deploy-dev:cloud-api:
  extends: .deploy-dev
  variables:
    PROJECT_PATH: cloud-api
    PROJECT_NAME: cloud-api
    IMAGE_NAME: $REGISTRY/cloud-api
    DEPLOYMENT_NAME: cloud-api
  needs:
    - build:cloud-api

deploy-prod:cloud-api:
  extends: .deploy-prod
  variables:
    PROJECT_PATH: cloud-api
    PROJECT_NAME: cloud-api
    IMAGE_NAME: $REGISTRY/cloud-api
    DEPLOYMENT_NAME: cloud-api

# Cloud Consumer 프로젝트
test:cloud-consumer:
  extends: .test
  variables:
    PROJECT_PATH: cloud-consumer

build:cloud-consumer:
  extends: .build-image
  variables:
    PROJECT_PATH: cloud-consumer
    IMAGE_NAME: $REGISTRY/cloud-consumer
  needs:
    - test:cloud-consumer

deploy-dev:cloud-consumer:
  extends: .deploy-dev
  variables:
    PROJECT_PATH: cloud-consumer
    PROJECT_NAME: cloud-consumer
    IMAGE_NAME: $REGISTRY/cloud-consumer
    DEPLOYMENT_NAME: cloud-consumer
  needs:
    - build:cloud-consumer

# Cloud Batch 프로젝트
test:cloud-batch:
  extends: .test
  variables:
    PROJECT_PATH: cloud-batch

build:cloud-batch:
  extends: .build-image
  variables:
    PROJECT_PATH: cloud-batch
    IMAGE_NAME: $REGISTRY/cloud-batch
  needs:
    - test:cloud-batch

deploy-dev:cloud-batch:
  extends: .deploy-dev
  variables:
    PROJECT_PATH: cloud-batch
    PROJECT_NAME: cloud-batch
    IMAGE_NAME: $REGISTRY/cloud-batch
    DEPLOYMENT_NAME: cloud-batch
  needs:
    - build:cloud-batch
```

### 3.3 환경별 파이프라인 분기 전략

#### 3.3.1 Merge Request 파이프라인

MR 생성 시 자동 실행되는 Job:

- **Test Job**: 단위 테스트 실행 및 커버리지 리포트 생성
- **Lint Job**: 코드 스타일 검사 (실패해도 파이프라인 계속 진행)
- **아티팩트**: 테스트 커버리지 HTML 리포트를 MR 페이지에 `expose_as`로 링크 제공

```yaml
rules:
  - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    changes:
      - $PROJECT_PATH/**/*
```

#### 3.3.2 개발 환경 배포 파이프라인

main/develop 브랜치에 push 시 자동 실행:

- **Test Job**: 테스트 실행
- **Build Job**: Docker 이미지 빌드 및 Registry 푸시
- **Deploy Dev Job**: 개발 환경에 자동 배포

```yaml
rules:
  - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
    changes:
      - $PROJECT_PATH/**/*
```

#### 3.3.3 운영 환경 배포 파이프라인

Git Tag 생성 시 수동 트리거:

- **Build Job**: 태그 버전으로 Docker 이미지 빌드
- **Deploy Prod Job**: 수동 승인 후 운영 환경 배포 (Semantic Versioning 형식 검증)

```yaml
rules:
  - if: '$CI_COMMIT_TAG =~ /^v[0-9]+\.[0-9]+\.[0-9]+$/'
    changes:
      - $PROJECT_PATH/**/*
```

### 3.4 아티팩트 공유 및 Environment 활용

#### 3.4.1 테스트 커버리지 리포트 공유

```yaml
artifacts:
  reports:
    coverage_report:
      coverage_format: cobertura
      path: $PROJECT_PATH/build/reports/jacoco/test/cobertura.xml
  paths:
    - $PROJECT_PATH/build/reports/jacoco/test/html/
  expose_as: 'Test Coverage Report'
```

- GitLab MR 페이지에서 "View exposed artifact" 링크로 HTML 리포트 직접 확인 가능
- Coverage 퍼센트는 MR 위젯에 표시

#### 3.4.2 Environment를 통한 배포 이력 관리

```yaml
environment:
  name: development/$PROJECT_NAME
  url: https://$PROJECT_NAME-dev.example.com
```

- GitLab Environments 페이지에서 배포 이력 추적
- 각 환경별 배포 상태 및 URL 확인 가능
- Rollback 기능 활용 가능

#### 3.4.3 dotenv를 통한 Job 간 변수 공유

```yaml
artifacts:
  reports:
    dotenv: build.env
```

빌드 Job에서 생성한 환경변수를 다운스트림 Job에 전달:

```bash
echo "IMAGE_TAG=$CI_COMMIT_SHA" >> build.env
echo "BUILD_TIME=$(date -u +%Y%m%d-%H%M%S)" >> build.env
```

### 3.5 확장 시나리오: SDK 배포

**`.deploy-sdk-template.yml`**

```yaml
.deploy-sdk:
  stage: deploy
  image: gradle:8-jdk17
  script:
    - cd $PROJECT_PATH
    - gradle publish -Pversion=$CI_COMMIT_TAG
  artifacts:
    reports:
      dotenv: sdk-publish.env
  environment:
    name: maven-repository
    url: https://maven.example.com/$PROJECT_NAME/$CI_COMMIT_TAG
  only:
    - tags
  rules:
    - if: '$CI_COMMIT_TAG =~ /^v[0-9]+\.[0-9]+\.[0-9]+$/'
      changes:
        - $PROJECT_PATH/**/*
```

사용 예시 (인증팀 모노레포):

```yaml
deploy-sdk:auth-sdk:
  extends: .deploy-sdk
  variables:
    PROJECT_PATH: auth-sdk
    PROJECT_NAME: auth-sdk
```

### 3.6 공통 라이브러리 변경 감지

모노레포 내 공통 라이브러리 변경 시 모든 프로젝트 빌드:

```yaml
# 공통 라이브러리를 사용하는 프로젝트의 rules 확장
test:cloud-api:
  extends: .test
  variables:
    PROJECT_PATH: cloud-api
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
      changes:
        - cloud-api/**/*
        - common-libs/**/*  # 공통 라이브러리 변경 시에도 실행
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
      changes:
        - cloud-api/**/*
        - common-libs/**/*
```

### 3.7 고도화: TypeScript CLI SDK

Shell 스크립트의 한계를 극복하기 위한 TypeScript 기반 CLI SDK 도입.

#### 3.7.1 Shell 스크립트의 한계

| 문제점               | 설명                            |
|-------------------|-------------------------------|
| **타입 안정성 부족**     | 변수 타입 체크 불가, 런타임 에러 발생 가능     |
| **복잡한 로직 구현 어려움** | JSON 파싱, 복잡한 조건문, 객체 지향 설계 불가 |
| **테스트 어려움**       | 단위 테스트 작성 및 실행이 복잡            |
| **디버깅 어려움**       | IDE 지원 부족, 에러 추적 어려움          |
| **재사용성 낮음**       | 함수 모듈화 및 패키지 관리 어려움           |
| **유지보수 어려움**      | 대규모 스크립트 관리 시 가독성 저하          |

#### 3.7.2 TypeScript CLI SDK 아키텍처

**프로젝트 구조:**

```
gitlab-cicd-templates/cli/
├── package.json
├── tsconfig.json
├── jest.config.js
├── src/
│   ├── index.ts
│   ├── cli.ts
│   ├── commands/
│   │   ├── test.command.ts
│   │   ├── build.command.ts
│   │   ├── deploy.command.ts
│   │   └── index.ts
│   ├── services/
│   │   ├── gradle.service.ts
│   │   ├── docker.service.ts
│   │   ├── kubernetes.service.ts
│   │   ├── coverage.service.ts
│   │   └── notification.service.ts
│   ├── utils/
│   │   ├── logger.ts
│   │   ├── config.ts
│   │   ├── shell.ts
│   │   └── validator.ts
│   └── types/
│       ├── config.types.ts
│       └── command.types.ts
├── dist/  (빌드 결과)
└── bin/
    └── cicd-cli
```

**`package.json`**

```json
{
  "name": "@org/gitlab-cicd-cli",
  "version": "1.0.0",
  "description": "GitLab CI/CD CLI SDK for monorepo projects",
  "main": "dist/index.js",
  "bin": {
    "cicd-cli": "./bin/cicd-cli"
  },
  "scripts": {
    "build": "tsc",
    "test": "jest",
    "lint": "eslint src/**/*.ts",
    "prepublish": "npm run build"
  },
  "dependencies": {
    "commander": "^11.0.0",
    "chalk": "^5.3.0",
    "ora": "^7.0.1",
    "execa": "^8.0.1",
    "fs-extra": "^11.1.1",
    "yaml": "^2.3.4",
    "zod": "^3.22.4"
  },
  "devDependencies": {
    "@types/node": "^20.10.0",
    "@types/jest": "^29.5.0",
    "typescript": "^5.3.0",
    "jest": "^29.7.0",
    "ts-jest": "^29.1.0",
    "eslint": "^8.55.0"
  }
}
```

#### 3.7.3 CLI SDK 구현 예시

**`src/cli.ts`**

```typescript
import {Command} from 'commander';
import {TestCommand} from './commands/test.command';
import {BuildCommand} from './commands/build.command';
import {DeployCommand} from './commands/deploy.command';
import {logger} from './utils/logger';

const program = new Command();

program
    .name('cicd-cli')
    .description('GitLab CI/CD CLI SDK')
    .version('1.0.0');

program
    .command('test')
    .description('Run tests with coverage')
    .requiredOption('-p, --project <path>', 'Project path')
    .option('-t, --threshold <number>', 'Coverage threshold', '80')
    .action(async (options) => {
        try {
            await TestCommand.execute(options);
        } catch (error) {
            logger.error('Test command failed', error);
            process.exit(1);
        }
    });

program
    .command('build')
    .description('Build Docker image')
    .requiredOption('-p, --project <path>', 'Project path')
    .requiredOption('-i, --image <name>', 'Image name')
    .option('-t, --tag <tag>', 'Image tag', process.env.CI_COMMIT_SHA)
    .action(async (options) => {
        try {
            await BuildCommand.execute(options);
        } catch (error) {
            logger.error('Build command failed', error);
            process.exit(1);
        }
    });

program
    .command('deploy')
    .description('Deploy to Kubernetes')
    .requiredOption('-d, --deployment <name>', 'Deployment name')
    .requiredOption('-i, --image <name>', 'Image name')
    .requiredOption('-t, --tag <tag>', 'Image tag')
    .requiredOption('-n, --namespace <namespace>', 'Kubernetes namespace')
    .requiredOption('-c, --context <context>', 'Kubernetes context')
    .option('--health-check', 'Run health check after deployment', true)
    .option('--rollback-on-failure', 'Rollback on deployment failure', true)
    .action(async (options) => {
        try {
            await DeployCommand.execute(options);
        } catch (error) {
            logger.error('Deploy command failed', error);
            process.exit(1);
        }
    });

program.parse();
```

**`src/commands/test.command.ts`**

```typescript
import {GradleService} from '../services/gradle.service';
import {CoverageService} from '../services/coverage.service';
import {logger} from '../utils/logger';
import ora from 'ora';

export interface TestOptions {
    project: string;
    threshold: string;
}

export class TestCommand {
    static async execute(options: TestOptions): Promise<void> {
        const spinner = ora('Running tests...').start();

        try {
            const gradleService = new GradleService(options.project);
            const coverageService = new CoverageService(options.project);

            // 테스트 실행
            await gradleService.runTests();
            spinner.succeed('Tests passed');

            // 커버리지 체크
            spinner.start('Checking coverage...');
            const coverage = await coverageService.getCoverage();
            const threshold = parseInt(options.threshold);

            if (coverage < threshold) {
                spinner.fail(`Coverage ${coverage}% is below threshold ${threshold}%`);
                process.exit(1);
            }

            spinner.succeed(`Coverage ${coverage}% meets threshold`);

            // 리포트 생성
            await coverageService.generateReport();
            logger.info('Coverage report generated');

        } catch (error) {
            spinner.fail('Test execution failed');
            throw error;
        }
    }
}
```

**`src/services/gradle.service.ts`**

```typescript
import {execa} from 'execa';
import {logger} from '../utils/logger';
import path from 'path';

export class GradleService {
    constructor(private projectPath: string) {
    }

    async runTests(): Promise<void> {
        logger.info(`Running tests for ${this.projectPath}...`);

        try {
            await execa('gradle', ['test', 'jacocoTestReport'], {
                cwd: path.resolve(this.projectPath),
                stdio: 'inherit',
            });
        } catch (error) {
            logger.error('Gradle tests failed', error);
            throw error;
        }
    }

    async buildJar(): Promise<string> {
        logger.info('Building JAR...');

        await execa('gradle', ['bootJar'], {
            cwd: path.resolve(this.projectPath),
            stdio: 'inherit',
        });

        // JAR 파일 경로 반환
        const jarPath = path.join(
            this.projectPath,
            'build/libs',
            `${path.basename(this.projectPath)}.jar`
        );

        return jarPath;
    }
}
```

**`src/services/kubernetes.service.ts`**

```typescript
import {execa} from 'execa';
import {logger} from '../utils/logger';

export interface DeployOptions {
    deployment: string;
    image: string;
    tag: string;
    namespace: string;
    context: string;
}

export class KubernetesService {
    async deploy(options: DeployOptions): Promise<void> {
        const {deployment, image, tag, namespace, context} = options;

        // Context 전환
        await this.useContext(context);

        // 현재 이미지 확인
        const currentImage = await this.getCurrentImage(deployment, namespace);
        logger.info(`Current image: ${currentImage}`);
        logger.info(`New image: ${image}:${tag}`);

        // 이미지 업데이트
        await execa('kubectl', [
            'set', 'image',
            `deployment/${deployment}`,
            `app=${image}:${tag}`,
            '-n', namespace,
        ], {stdio: 'inherit'});

        // Rollout 상태 확인
        await this.waitForRollout(deployment, namespace);
    }

    async rollback(deployment: string, namespace: string): Promise<void> {
        logger.warn(`Rolling back deployment ${deployment}...`);

        await execa('kubectl', [
            'rollout', 'undo',
            `deployment/${deployment}`,
            '-n', namespace,
        ], {stdio: 'inherit'});
    }

    async healthCheck(deployment: string, namespace: string): Promise<boolean> {
        try {
            const {stdout} = await execa('kubectl', [
                'get', 'deployment', deployment,
                '-n', namespace,
                '-o', 'jsonpath={.status.conditions[?(@.type=="Available")].status}',
            ]);

            return stdout === 'True';
        } catch (error) {
            logger.error('Health check failed', error);
            return false;
        }
    }

    private async useContext(context: string): Promise<void> {
        await execa('kubectl', ['config', 'use-context', context]);
    }

    private async getCurrentImage(deployment: string, namespace: string): Promise<string> {
        const {stdout} = await execa('kubectl', [
            'get', 'deployment', deployment,
            '-n', namespace,
            '-o', 'jsonpath={.spec.template.spec.containers[0].image}',
        ]);

        return stdout;
    }

    private async waitForRollout(deployment: string, namespace: string): Promise<void> {
        await execa('kubectl', [
            'rollout', 'status',
            `deployment/${deployment}`,
            '-n', namespace,
            '--timeout=5m',
        ], {stdio: 'inherit'});
    }
}
```

#### 3.7.4 템플릿에서 CLI SDK 활용

**CLI SDK를 사용하는 템플릿:**

```yaml
.test:
  stage: test
  image: node:20-alpine
  before_script:
    - apk add --no-cache git bash openjdk17
    - npm install -g @org/gitlab-cicd-cli
  script:
    - cicd-cli test --project $PROJECT_PATH --threshold ${COVERAGE_THRESHOLD:-80}
  artifacts:
    reports:
      junit: $PROJECT_PATH/build/test-results/test/*.xml
      coverage_report:
        coverage_format: cobertura
        path: $PROJECT_PATH/build/reports/jacoco/test/cobertura.xml
    paths:
      - $PROJECT_PATH/build/reports/jacoco/test/html/
    expose_as: 'Test Coverage Report'
```

```yaml
.build-image:
  stage: build
  image: node:20-alpine
  services:
    - docker:dind
  before_script:
    - apk add --no-cache git bash openjdk17 docker
    - npm install -g @org/gitlab-cicd-cli
  script:
    - cicd-cli build --project $PROJECT_PATH --image $IMAGE_NAME --tag $CI_COMMIT_SHA
  artifacts:
    reports:
      dotenv: build.env
```

```yaml
.deploy-dev:
  stage: deploy
  image: node:20-alpine
  before_script:
    - apk add --no-cache git bash kubectl
    - npm install -g @org/gitlab-cicd-cli
  environment:
    name: development/$PROJECT_NAME
    url: https://$PROJECT_NAME-dev.example.com
  script:
    - cicd-cli deploy
      --deployment $DEPLOYMENT_NAME
      --image $IMAGE_NAME
      --tag $CI_COMMIT_SHA
      --namespace dev
      --context dev-cluster
      --health-check
      --rollback-on-failure
```

#### 3.7.5 CLI SDK의 장점

| 장점          | 설명                                    |
|-------------|---------------------------------------|
| **타입 안정성**  | TypeScript 타입 시스템으로 컴파일 타임 에러 방지      |
| **테스트 용이성** | Jest를 활용한 단위 테스트 및 통합 테스트 작성          |
| **디버깅 편의성** | IDE 지원, 소스맵, 스택 트레이스                  |
| **모듈화**     | 명확한 책임 분리 (Services, Commands, Utils) |
| **재사용성**    | npm 패키지로 배포, 버전 관리 용이                 |
| **확장성**     | 플러그인 아키텍처, 커스텀 명령어 추가 용이              |
| **유지보수성**   | 대규모 로직도 객체지향 설계로 관리                   |
| **에러 핸들링**  | try-catch, Promise, async/await 활용    |

#### 3.7.6 마이그레이션 전략

**Phase 1: Shell 스크립트 유지 (현재)**

- 단순한 로직은 Shell 스크립트로 유지
- 빠른 구현 및 검증

**Phase 2: 하이브리드 (전환기)**

- 복잡한 로직만 CLI SDK로 전환
- Shell 스크립트와 CLI 동시 지원

**Phase 3: CLI SDK 완전 전환 (목표)**

- 모든 스크립트를 CLI SDK로 통합
- Shell 스크립트는 Deprecated 처리

## 4. 구현 계획

### 4.1 Phase 1: 공통 템플릿 프로젝트 구축 (Shell 스크립트 기반)

**주요 작업:**

- gitlab-cicd-templates 프로젝트 생성
- 6개 템플릿 구현 (test, lint, build-image, deploy-dev, deploy-prod, deploy-sdk)
- Shell 스크립트 작성 및 테스트
    - `scripts/test/run-test.sh`: 테스트 실행 및 커버리지 체크
    - `scripts/build/build-docker-image.sh`: Docker 이미지 빌드
    - `scripts/deploy/deploy-k8s.sh`: Kubernetes 배포
    - `scripts/deploy/health-check.sh`: 배포 후 헬스체크
    - `scripts/common/notify-slack.sh`: Slack 알림
- 템플릿 문서화 및 사용 가이드 작성

**결과물:**

- `.test-template.yml`: 테스트 및 커버리지 리포트 생성
- `.lint-template.yml`: 코드 품질 검사
- `.build-image-template.yml`: Docker 이미지 빌드
- `.deploy-dev-template.yml`: 개발 환경 배포
- `.deploy-prod-template.yml`: 운영 환경 배포
- `.deploy-sdk-template.yml`: SDK/라이브러리 배포
- Shell 스크립트 세트 (10개 내외)

### 4.2 Phase 2: 파일럿 팀 적용

**주요 작업:**

- 클라우드팀 모노레포에 우선 적용
- 단일 프로젝트(cloud-api)에 템플릿 적용 및 검증
- MR 파이프라인, 개발 배포, 운영 배포 시나리오 테스트
- 아티팩트 공유 및 Environment 기능 검증

**검증 항목:**

- 경로 기반 변경 감지 정확성
- MR에서 테스트 커버리지 리포트 노출
- 개발/운영 환경 자동/수동 배포 동작
- 공통 라이브러리 변경 시 관련 프로젝트 빌드

### 4.3 Phase 3: 전체 팀 확대

**주요 작업:**

- 클라우드팀 모든 프로젝트 적용 (consumer, batch)
- 인증팀 모노레포 적용
- SDK 배포 템플릿 적용 (auth-sdk)
- 팀별 특수 요구사항 수용을 위한 템플릿 확장

**확장 시나리오:**

- 각 팀별 커스텀 rules 추가
- 팀별 특화 스크립트 개발 (데이터베이스 마이그레이션, 캐시 초기화 등)
- 다중 클러스터 배포 지원

### 4.4 Phase 4: 최적화 및 모니터링

**주요 작업:**

- 빌드 시간 분석 및 최적화 (캐싱 전략, 병렬 실행)
- 파이프라인 실패율 모니터링 및 알림 설정
- 템플릿 버전 관리 전략 수립 (Semantic Versioning)
- 공통 템플릿 변경 시 Canary 배포 프로세스

**모니터링 지표:**

- 평균 파이프라인 실행 시간
- 파이프라인 성공률
- 환경별 배포 빈도
- 아티팩트 스토리지 사용량

### 4.5 Phase 5 (고도화): TypeScript CLI SDK 도입

**주요 작업:**

- CLI SDK 프로젝트 구조 설계 및 초기 설정
- 핵심 Commands 구현
    - `test.command.ts`: 테스트 실행 로직
    - `build.command.ts`: 빌드 로직
    - `deploy.command.ts`: 배포 로직
- 핵심 Services 구현
    - `gradle.service.ts`: Gradle 작업 추상화
    - `docker.service.ts`: Docker 작업 추상화
    - `kubernetes.service.ts`: K8s 작업 추상화
    - `notification.service.ts`: Slack/Jira 알림
- 단위 테스트 및 통합 테스트 작성 (Jest)
- npm 패키지로 배포 (`@org/gitlab-cicd-cli`)

**결과물:**

- TypeScript CLI SDK (npm 패키지)
- 템플릿 v2 (CLI SDK 사용)
- 단위 테스트 커버리지 80% 이상
- 마이그레이션 가이드 문서

**마이그레이션 타임라인:**

1. **Week 1-2**: CLI SDK 기본 구조 및 Test Command 구현
2. **Week 3-4**: Build/Deploy Command 구현
3. **Week 5-6**: 파일럿 팀 적용 및 검증
4. **Week 7-8**: 전체 팀 마이그레이션
5. **Week 9-10**: Shell 스크립트 Deprecated 처리 및 문서화

**마이그레이션 시 고려사항:**

- Shell 스크립트와 CLI SDK 병행 지원 (최소 3개월)
- 각 팀별 마이그레이션 일정 조율
- Breaking Change 최소화 (동일한 API 유지)
- Rollback 계획 수립

## 5. 핵심 특징 요약

### 5.1 환경별 파이프라인 분기

| 환경     | 트리거 조건                                         | 실행 Job                  | 특징                 |
|--------|------------------------------------------------|-------------------------|--------------------|
| **MR** | `CI_PIPELINE_SOURCE == "merge_request_event"`  | Test, Lint              | 커버리지 리포트 MR에 자동 첨부 |
| **개발** | `CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH`       | Test, Build, Deploy Dev | 자동 배포              |
| **운영** | `CI_COMMIT_TAG =~ /^v[0-9]+\.[0-9]+\.[0-9]+$/` | Build, Deploy Prod      | 수동 승인 배포           |

### 5.2 아티팩트 활용

| 아티팩트 유형             | 용도          | 활용 방법                    |
|---------------------|-------------|--------------------------|
| **JUnit Report**    | 테스트 결과      | GitLab UI에서 실패한 테스트 확인   |
| **Coverage Report** | 커버리지 분석     | Cobertura 형식으로 MR 위젯에 표시 |
| **HTML Report**     | 상세 커버리지     | `expose_as`로 MR에 링크 제공   |
| **dotenv**          | Job 간 변수 공유 | 빌드 정보를 배포 Job에 전달        |

### 5.3 공통 템플릿 재사용 구조

```
각 팀 모노레포 .gitlab-ci.yml
  ├─ include: gitlab-cicd-templates 프로젝트
  ├─ extends: Hidden Job 템플릿
  └─ variables: 프로젝트별 설정 주입
```

단순히 `extends`와 `variables`만으로 여러 프로젝트에 일관된 파이프라인 적용

## 6. 성공 지표

- **빌드 시간 단축**: 변경 없는 프로젝트 Job 생략으로 평균 빌드 시간 30% 감소
- **중복 코드 제거**: 각 팀 `.gitlab-ci.yml` 크기 50% 감소 (extends 패턴 활용)
- **파이프라인 성공률**: 95% 이상 유지
- **신규 프로젝트 온보딩 시간**: 1일 이내
- **MR 리뷰 효율성**: 커버리지 리포트 자동 첨부로 리뷰 시간 20% 단축

## 7. 위험 요소 및 대응

| 위험 요소                                 | 영향 | 대응 방안                                  |
|---------------------------------------|----|----------------------------------------|
| 공통 템플릿 변경 시 전체 팀 프로젝트 영향              | 높음 | 템플릿 버전 관리 (ref 지정), 점진적 롤아웃, Canary 배포 |
| 경로 기반 감지 오류 (false positive/negative) | 중간 | 철저한 테스트, 공통 라이브러리 변경 시 rules 확장        |
| 팀별 특수 요구사항 수용 어려움                     | 중간 | 템플릿 오버라이드 메커니즘 제공, 팀별 커스텀 스크립트 지원      |
| 공통 템플릿 프로젝트 권한 관리                     | 낮음 | 명확한 Maintainer 정의 및 변경 승인 프로세스, MR 필수화 |
| 아티팩트 스토리지 용량 증가                       | 낮음 | 아티팩트 보관 기간 정책 수립, 불필요한 아티팩트 정리         |

## 8. 템플릿 버전 관리 전략

### 8.1 버전 참조 방식

**안정 버전 사용 (권장):**

```yaml
include:
  - project: 'cicd/gitlab-cicd-templates'
    ref: v1.2.0  # 특정 태그 버전
    file: '/templates/.test-template.yml'
```

**최신 버전 사용 (주의):**

```yaml
include:
  - project: 'cicd/gitlab-cicd-templates'
    ref: main  # 항상 최신 버전 (Breaking Change 위험)
    file: '/templates/.test-template.yml'
```

### 8.2 템플릿 변경 프로세스

1. **gitlab-cicd-templates 프로젝트에서 변경 작업**
2. **파일럿 팀에서 테스트 (ref: feature-branch 사용)**
3. **검증 완료 후 main 브랜치 병합**
4. **새로운 버전 태그 생성 (예: v1.3.0)**
5. **각 팀에 버전 업그레이드 안내 및 마이그레이션 가이드 제공**

## 9. 참고 자료

- [GitLab CI/CD Include Documentation](https://docs.gitlab.com/ee/ci/yaml/#include)
- [GitLab CI/CD Rules Documentation](https://docs.gitlab.com/ee/ci/yaml/#rules)
- [GitLab CI/CD Artifacts Documentation](https://docs.gitlab.com/ee/ci/yaml/#artifacts)
- [GitLab Environments and Deployments](https://docs.gitlab.com/ee/ci/environments/)
- [Monorepo CI/CD Best Practices](https://docs.gitlab.com/ee/ci/pipelines/pipeline_efficiency.html)
