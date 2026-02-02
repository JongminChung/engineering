# Kustomize

## 프로젝트 구조 (Project Structure)

Kustomize는 일반적으로 베이스(base) 매니페스트와 환경별 오버레이(overlay)를 분리하여 관리합니다.

```text
.
├── base
│   ├── kustomization.yml
│   └── deploy.yml
├── kustomization.yml
└── configmap.yml
```

---

## 다중 환경 배포 구조 (Multi-Environment Structure)

Kustomize를 사용하여 개발(Staging), 운영(Production) 등 여러 환경의 설정을 효율적으로 관리할 수 있습니다.

### 디렉토리 구조

```text
.
├── base/
│   ├── kustomization.yml
│   └── deploy.yml
└── overlays/
    ├── staging/
    │   └── kustomization.yml
    └── production/
        └── kustomization.yml
```

### 1. Base 설정 (`base/`)

공통적으로 사용되는 기본 매니페스트입니다.

**base/deploy.yml**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
    name: my-app
spec:
    replicas: 1
    selector:
        matchLabels:
            app: my-app
    template:
        metadata:
            labels:
                app: my-app
        spec:
            containers:
                - name: app
                  image: my-app:latest
```

**base/kustomization.yml**

```yaml
resources:
    - deploy.yml
```

### 2. 환경별 오버레이 (`overlays/`)

`base` 설정을 상속받아 각 환경에 맞게 패치합니다.

**staging/kustomization.yml**

```yaml
resources:
    - ../../base
namePrefix: staging-
commonLabels:
    env: staging
```

**production/kustomization.yml**

```yaml
resources:
    - ../../base
namePrefix: prod-
commonLabels:
    env: production
replicas:
    - name: my-app
      count: 3
```

---

## 원격 리소스 참조 (Remote Resources)

`resources` 필드에서 로컬 파일뿐만 아니라 HashiCorp, GitHub, S3 등 외부 소스의 리소스를 직접 참조할 수 있습니다.

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
    - hashicorp/consul/aws # HashiCorp URL format
    - github.com/org/repo/mysql?ref=v1.2.3 # GitHub (tag/branch)
    - s3::https://s3-region.amazonaws.com/bucket/key # S3 Bucket
```

---

## 이미지 업데이트 (Images)

`images` 필드를 사용하여 컨테이너 이미지를 쉽게 업데이트할 수 있습니다.

```yaml
images:
    - name: lordoftherjars/pacman-kikd
      newTag: 1.0.1
```

---

## ConfigMap 생성 (configMapGenerator)

`configMapGenerator`로 설정 파일이나 리터럴을 ConfigMap으로 생성할 수 있습니다.

```yaml
configMapGenerator:
    - name: app-config
      files:
          - config/app.json
      literals:
          - LOG_LEVEL=info
```

JSON 파일은 `//` 주석을 허용합니다.

---

## ConfigMap 변경 및 배포 흐름

`configMapGenerator`는 내용 해시를 이름에 포함하므로, 파일/리터럴이 바뀌면 새로운 ConfigMap이 생성되고 참조 리소스(Deployment 등)가 자동으로 갱신되어 롤링 업데이트가 트리거됩니다.

### 변경 방법

1. **원본 수정**: `files`에 지정한 파일 또는 `literals` 값을 수정합니다.
2. **결과 확인**:

```bash
kustomize build .
```

### 적용 및 배포

로컬 적용:

```bash
kubectl apply -k .
```

GitOps(Argo CD) 적용:

1. 변경 사항 커밋/푸시
2. Argo CD가 자동 동기화하거나, 수동 동기화 실행

---

## 패치 (Patches)

`patches`를 사용하여 임의의 쿠버네티스 필드를 업데이트할 수 있습니다.

### JSON Patch (RFC 6902)

`op: add`, `op: replace`, `op: remove` 등을 사용하여 정밀하게 필드를 제어합니다.

```yaml
patches:
    - target:
          kind: Deployment
          name: pacman-kikd
      patch: |-
          - op: replace
            path: /spec/replicas
            value: 3
          - op: add
            path: /metadata/labels/env
            value: prod
```

---

## 실행 및 확인 (Dry-run)

배포하기 전에 미리 변경 사항을 확인할 수 있습니다.

```bash
# 로컬에서 빌드된 결과 확인
kustomize build .

# 쿠버네티스 클러스터에 적용될 내용 미리보기 (dry-run)
kubectl apply -k . --dry-run=client -o yaml
```

---

## Git 관리 및 GitOps 워크플로우

GitOps 환경에서 Kustomize 설정은 다음과 같이 관리합니다.

1. **변경 사항 작업**: `kustomization.yml` 또는 리소스 파일 수정.
2. **검증**: `kustomize build` 명령어로 생성된 매니페스트 확인.
3. **커밋 및 푸시**:
    ```bash
    git add .
    git commit -m "feat: update image tag to 1.0.1 and scale replicas"
    git push origin main
    ```
4. **동기화**: Argo CD GitOps 도구가 변경 사항을 감지하여 클러스터에 자동 반영합니다.

---
