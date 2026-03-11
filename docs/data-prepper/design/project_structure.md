# OpenSearch Data Prepper 프로젝트 구조 및 설계 가이드

이 문서는 OpenSearch Data Prepper의 전체적인 설계 구조와 모듈 간 의존 관계를 설명합니다. Ingestion 시스템을 설계할 때 참고할 수 있도록 주요 컴포넌트와 데이터 흐름, 그리고 확장성을 위한 플러그인 아키텍처를 중심으로 정리되었습니다.

- 더 자세한 플러그인 아키텍처 설계 원칙은 [플러그인 아키텍처 가이드](plugin_architecture_guide.md)를 참고하세요.

## 1. 파이프라인 아키텍처 (Pipeline Architecture)

Data Prepper는 YAML 설정 파일을 통해 데이터 파이프라인을 정의합니다. 하나의 파이프라인은 다음과 같은 4가지 핵심 컴포넌트로 구성됩니다.

- **Source (소스):** 데이터의 입력 지점입니다. HTTP, Kafka, S3 등 다양한 소스로부터 데이터를 소비합니다.
- **Buffer (버퍼):** 소스와 프로세서 사이의 완충 지대입니다. 기본적으로 메모리 기반의 `bounded_blocking` 큐를 사용하며, 데이터의 안정적인 전달을 보장합니다.
- **Processor (프로세서):** 데이터를 필터링, 변환, 강화(enrich)하는 중간 단계입니다. 여러 프로세서를 체인 형태로 연결하여 사용할 수 있습니다.
- **Sink (싱크):** 처리된 데이터의 목적지입니다. OpenSearch, S3 등으로 데이터를 내보냅니다.

---

## 2. 프로젝트 모듈 구조 및 역할

프로젝트는 기능별로 Gradle 멀티 모듈로 나뉘어 있으며, 각 모듈은 명확한 책임을 가집니다.

| 모듈명                              | 주요 역할                                                                       |
| :---------------------------------- | :------------------------------------------------------------------------------ |
| **`data-prepper-main`**             | 애플리케이션 진입점. Spring Context를 초기화하고 실행을 시작합니다.             |
| **`data-prepper-core`**             | 엔진의 핵심 로직. YAML 설정 파싱 및 파이프라인 생명주기 관리.                   |
| **`data-prepper-api`**              | 플러그인 개발을 위한 공통 인터페이스 및 데이터 모델(`Event`, `Record` 등) 정의. |
| **`data-prepper-plugin-framework`** | 플러그인 검색, 로딩, 의존성 주입(DI) 및 Spring Bean 격리 관리.                  |

---

## 3. 플러그인 프레임워크 (Plugin Framework) 상세 역할

`data-prepper-plugin-framework`는 단순한 라이브러리가 아니라 플러그인의 생명주기와 실행 환경을 관리하는 핵심 인프라입니다.

### 3.1 플러그인 로딩 및 검색 (Discovery)

- **`PluginProvider`:** 클래스패스에서 `@DataPrepperPlugin` 어노테이션이 붙은 클래스들을 스캔하여 찾습니다.
- **`DefaultPluginFactory`:** 찾은 클래스를 기반으로 실제 플러그인 인스턴스를 생성하는 메인 팩토리입니다. YAML 설정값과 매핑되는 설정 객체를 생성하고 주입합니다.

### 3.2 Spring Bean 격리 및 관리 (Context Isolation)

Data Prepper는 플러그인 간의 설정 충돌을 방지하기 위해 **계층적 Spring Context**를 사용합니다.

- **격리된 Context 생성:** 각 플러그인 인스턴스가 생성될 때마다 `PluginBeanFactoryProvider`는 해당 플러그인만을 위한 전용 `ApplicationContext`를 생성합니다.
- **Bean 공유 제어:** 플러그인은 공통 기능(Public Context)에는 접근할 수 있지만, 다른 플러그인의 내부 Bean이나 엔진 핵심(Core)의 내부 Bean에는 직접 접근할 수 없도록 격리됩니다.
- **설정 주입:** 플러그인 생성 시 필요한 의존성(설정 객체, 메트릭, API 등)을 자동으로 주입(DI)해 줍니다.

### 3.3 확장 기능 (Extensions)

- 엔진의 기능을 전역적으로 확장하는 `ExtensionPlugin`을 로드하고 실행합니다. 이는 인증(Auth)이나 공통 설정 관리와 같이 여러 플러그인에 걸쳐 공통적으로 필요한 기능을 제공할 때 사용됩니다.

---

## 4. 의존성 설계 (Dependency Design)

의존성 흐름은 상위 실행부에서 하위 API 및 모델 방향으로 단방향으로 흐르도록 설계되어 있습니다.

### 4.1 전체 의존성 흐름 (Top-Down)

1. **`data-prepper-main`**
    - `data-prepper-core` 의존
    - `data-prepper-plugins` 의존 (모든 플러그인 포함)
2. **`data-prepper-core`**
    - `data-prepper-plugin-framework` 의존
    - `data-prepper-api` 의존
3. **`data-prepper-plugins` (Aggregation)**
    - 내부의 모든 하위 플러그인(`opensearch`, `s3`, `http` 등)을 `api` 프로젝트로 포함합니다.
    - 이 구조 덕분에 `main` 모듈은 `plugins` 모듈 하나만 의존해도 모든 기능을 즉시 사용할 수 있는 'Fat Binary' 형태를 갖춥니다.

### 4.2 플러그인의 독립성

각 개별 플러그인 모듈은 `data-prepper-core`를 직접 참조하지 않습니다. 대신 `data-prepper-api`를 참조하여 인터페이스만 구현합니다. 이는 엔진 내부 구현과 플러그인 구현을 분리하여
결합도를 낮추고 확장성을 높이는 핵심 설계 원칙입니다.

---

## 5. 애플리케이션 실행 흐름

1. **진입점 실행:** `DataPrepperExecute` (main 클래스)에서 시작.
2. **Context 구성:** `AbstractContextManager`를 통해 계층형 Spring Application Context를 생성합니다.
    - `Public Context`: 공통 빈 (Event, Expression 등)
    - `Core Context`: 엔진 핵심 빈 (DataPrepper 등)
    - `Plugin Context`: 각 플러그인별 격리된 빈 관리
3. **파이프라인 생성:** `PipelineTransformer`가 YAML 설정을 읽어 Java 객체 기반의 파이프라인으로 변환합니다.
4. **엔진 구동:** `DataPrepper.execute()`가 호출되어 각 파이프라인의 수집 및 처리가 시작됩니다.

---

## 6. 설계 시 참고할 포인트 (Best Practices)

1. **Interface 기반 설계:** `api` 모듈을 분리하여 엔진과 구현체 사이의 접점을 최소화합니다.
2. **모듈화를 통한 확장성:** 새로운 기능을 추가할 때 기존 코드를 수정하지 않고 새로운 플러그인 모듈을 추가하는 것만으로 기능을 확장할 수 있습니다.
3. **의존성 격리:** 플러그인이 엔진의 내부 로직에 접근하지 못하도록 하여, 엔진의 버전 업그레이드가 플러그인에 미치는 영향을 최소화합니다.
4. **계층적 Context 관리:** Spring의 부모-자식 Context 구조를 사용하여 설정 간섭을 방지하고 공통 자원을 효율적으로 공유합니다.
