# Spectral OpenAPI Style Guide PoC

## 목표

- Stoplight OpenAPI Style Guide ruleset(`spectral:oas`) 적용 여부 확인
- $ref 기반으로 분리된 PetStore 스펙의 해석/해결 검증

## 구성 파일

- `study/spectral/petstore.yaml`
- `study/spectral/paths/pets.yaml`
- `study/spectral/components/parameters/Limit.yaml`
- `study/spectral/components/responses/PetsResponse.yaml`
- `study/spectral/components/responses/PetResponse.yaml`
- `study/spectral/components/responses/ErrorResponse.yaml`
- `study/spectral/components/schemas/Pet.yaml`
- `study/spectral/components/schemas/NewPet.yaml`
- `study/spectral/components/schemas/Category.yaml`
- `study/spectral/components/schemas/Tag.yaml`
- `study/spectral/components/schemas/Pets.yaml`
- `study/spectral/components/schemas/Error.yaml`
- `study/spectral/scripts/run-spectral.ts`
- `study/spectral/ruleset.ts`
- `study/spectral/package.json`

## 설치

```bash
pnpm install
```

## 실행 (workspace script)

```bash
pnpm --filter spectral-poc run spectral:check
```

## Node 버전 주의

- 저장소 기본 설정은 `engine-strict=true`라서 Node 24가 필요함
- Node 24가 아니면 아래처럼 실행할 때만 예외 처리 가능

```bash
pnpm --config.engine-strict=false --filter spectral-poc run spectral:check
```

## 동작 요약

- `@stoplight/spectral-rulesets`의 `oas` 규칙셋으로 OpenAPI Style Guide 검증 수행
- `study/spectral/ruleset.ts`에서 `oas`를 확장하고 커스텀 룰 추가
- `@stoplight/spectral-ref-resolver`로 파일 경로 기반 `$ref` 해석
- 결과는 severity, code, message, path 기준으로 출력

## $ref 해석 흐름

- `petstore.yaml` → `paths/pets.yaml`
- `paths/pets.yaml` → `petstore.yaml#/components/*`
- `components/responses/*` → `petstore.yaml#/components/schemas/*`
- `components/schemas/NewPet.yaml` → `petstore.yaml#/components/schemas/Pet`
- `components/schemas/Pet.yaml` → `petstore.yaml#/components/schemas/Category`,
  `Tag`

## 규칙 검증 확인 방법

- 현재 스펙은 기본 규칙을 통과하도록 작성됨
- 규칙 위반을 확인하려면 아래 중 하나를 임시로 변경
- `study/spectral/petstore.yaml`에서 `info.contact` 제거 → `info-contact` 위반
- `study/spectral/paths/pets.yaml`에서 `operationId` 제거 → `operation-operationId`
  위반

## 규칙 위반 예시 (출력 샘플)

### 예시 1) info.contact 제거

변경:

```yaml
info:
    title: PetStore API
    version: 1.0.0
    description: Minimal PetStore spec for Spectral PoC.
    license:
        name: Apache-2.0
        url: https://www.apache.org/licenses/LICENSE-2.0.html
```

예상 출력:

```text
[warn] info-contact: Info object must have "contact" object.
  at: /.../study/spectral/petstore.yaml#/info
```

### 예시 2) operationId 제거

변경:

```yaml
get:
    summary: List pets
    description: Return a paged list of pets.
```

예상 출력:

```text
[warn] operation-operationId: Operation must have "operationId".
  at: /.../study/spectral/paths/pets.yaml#/get

### 예시 3) info.x-service 누락 (커스텀 룰)

변경:

```yaml
info:
    title: PetStore API
    version: 1.0.0
    description: Minimal PetStore spec for Spectral PoC.
    contact:
        name: Engineering
```

예상 출력:

```text
[warn] info-x-service: Info object must have "x-service" extension.
  at: /.../study/spectral/petstore.yaml#/info/x-service
```

## 실행 출력 로그 캡처

### 1) info.contact 제거 후 실행

```text
[warn] info-contact: Info object must have "contact" object.
  at: /Users/jongminchung/Documents/engineering-spectral/study/spectral/petstore.yaml#/info
```

### 2) GET /pets operationId 제거 후 실행

```text
[warn] operation-operationId: Operation must have "operationId".
  at: /Users/jongminchung/Documents/engineering-spectral/study/spectral/paths/pets.yaml#/get
```
