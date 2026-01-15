# F/U

**_Table of Contents_**

<!-- TOC -->

- [F/U](#fu)
    - [spring boot 4.0.x](#spring-boot-40x) - [Module Dependencies](#module-dependencies) - [1. 배경 및 문제점](#1-배경-및-문제점) - [2. 주요 변경 사항 (Modularization)](#2-주요-변경-사항-modularization) - [3. 영향 및 권장 사항](#3-영향-및-권장-사항) - [4. Q&A: "결국 다 가지고 있는 것이 문제인가?"](#4-qa-결국-다-가지고-있는-것이-문제인가) - [5. 그래서 어떻게 모듈을 개발해야 할까? (모듈 개발 가이드)](#5-그래서-어떻게-모듈을-개발해야-할까-모듈-개발-가이드)
        <!-- TOC -->

## spring boot 4.0.x

### [Module Dependencies](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#module-dependencies)

Spring Boot 4에서 도입된 새로운 모듈식 설계의 핵심은 거대해진 `spring-boot-autoconfigure` 모듈을 분해하여
유지보수성과 모듈성을 높임.

([관련 블로그 글](https://spring.io/blog/2025/10/28/modularizing-spring-boot)요약)

#### 1. 배경 및 문제점

- **비대해진 autoconfigure**: 기존 `spring-boot-autoconfigure` 모듈은 Spring이 지원하는 모든 기술에
  대한 자동 설정 로직을 담고 있어, 기술이 추가될 때마다 모듈의 크기와 복잡도가 무한히 증가함.
- **의존성 오염**: 특정 기술을 사용하지 않더라도 해당 기술의 자동 설정 클래스가 클래스패스에 포함되어 잠재적인 의존성 문제를 야기할 수
  있음.
- **모듈성 결여**: 모듈 간의 경계가 불분명하여 내부 구현의 변경이 광범위한 영향을 미칠 수 있는 구조.

**모듈이 필요한 이유**는 다음과 같다.

- 모듈 경계는 느슨한 규칙이 아니라 계약이 됨.
- 아티팩트 크기 감소: 사용하지 않을 수도 있는 많은 기능을 담은 하나의 큰 autoconfigure JAR을 배포하는 대신, 애플리케이션은
  관련 모듈만 가져오도록함.

#### 2. 주요 변경 사항 (Modularization)

- **모듈 분리**: `spring-boot-autoconfigure` 내부에 혼재되어 있던 자동 설정 로직들을 각각의 전용 모듈(예:
  `spring-boot-autoconfigure-data-jpa`, `spring-boot-autoconfigure-web-servlet`
  등)로 분리했다.
- **명확한 의존성 정의**: 각 자동 설정 모듈은 자신이 담당하는 기술에 필요한 의존성만을 명확히 정의한다.
- **스타터(Starter) 구조 개선**: 이제 각 기술별 스타터는 전체 autoconfigure 대신 필요한 세부 autoconfigure
  모듈만을 참조한다.

#### 3. 영향 및 권장 사항

- **스타터 사용 권장**: 기술 도입 시 `spring-boot-starter-*` 형태의 스타터를 사용하면 필요한 모듈만 자동으로
  포함됨.
- **직접 참조 주의**: 만약 프로젝트에서 `spring-boot-autoconfigure`를 직접 의존성으로 추가했거나, 특정 자동 설정
  클래스를 수동으로 가져와 사용했다면, 새롭게 분리된 모듈로 의존성을 교체해야 함.
- **테스트 최적화**: 테스트 시에도 필요한 모듈만 포함되므로, 더 가볍고 명확한 테스트 환경 구성이 가능해짐.

#### 4. Q&A: "결국 다 가지고 있는 것이 문제인가?"

사용자의 질문: "spring-boot-autoconfigure가 각각 모듈화해서 개발했지만 이를 모두 가지고 있는 것이 문제인가?"

- **핵심 답변**: 맞습니다. 정확히는 **'하나의 거대한 JAR 파일(`spring-boot-autoconfigure.jar`) 안에 모든
  기술의 자동 설정 로직과 선택적 의존성(optional dependencies)이 뒤섞여 있는 것'**이 문제였습니다.
- **상세 설명**:
    - **과거의 문제**: 수백 개의 서로 다른 기술(Data JPA, MongoDB, Redis, Web, LDAP 등)에 대한 설정이 한
      곳에 모여 있어, 특정 기술 하나만 써도 전체 설정 코드가 클래스패스에 로드되었습니다. 이는 빌드 속도 저하, 의존성 충돌, 유지보수의
      어려움을 야기했습니다.
    - **현재의 해결책**: Spring Boot 4에서는 이 "모두"를 각각의 전용 모듈로 쪼갰습니다. 이제 애플리케이션은 자기가 사용하는
      기술에 해당하는 `spring-boot-autoconfigure-xxx` 모듈만 가져옵니다.
    - **결론**: 전체 자동 설정 기능은 여전히 존재하지만, 더 이상 **하나의 거대한 덩어리**로 존재하지 않고 **필요한 조각만 골라 쓸
      수 있는 구조**로 바뀌었습니다.

**결론적으로**, Spring Boot 4의 모듈화는 "필요한 것만 사용하고, 명확하게 관리한다"는 원칙을 강화하여
프레임워크의 지속 가능성과 사용자의 프로젝트 제어력을 향상시키는 중요한 변화임.

#### 5. 그래서 어떻게 모듈을 개발해야 할까? (모듈 개발 가이드)

Spring Boot 4의 설계 철학을 바탕으로 새로운 모듈(또는 라이브러리)을 개발할 때 고려해야 할 원칙임.

- **자동 설정(Auto-configuration)의 분리**:
    - 라이브러리 핵심 로직(Core)과 Spring Boot 자동 설정 로직을 분리.
    - 예: `my-library-core` + `my-library-spring-boot-autoconfigure`
- **기술별 세분화 (Fine-grained Modules)**:
    - 하나의 거대한 `autoconfigure` 모듈을 만드는 대신, 지원하는 기술별로 모듈을 쪼개야 함.
    - 예: `my-lib-autoconfigure-jpa`, `my-lib-autoconfigure-web` 등. 사용자가 필요한
      의존성만 선택적으로 가져가게 함.
- **명확한 의존성 경계 설정**:
    - `api`와 `implementation` 의존성을 명확히 구분하여, 사용자에게 불필요한 전이 의존성(transitive
      dependencies)이 노출되지 않도록 해야함.
    - 자동 설정 모듈은 해당 기술이 클래스패스에 있을 때만 활성화되도록 `@ConditionalOnClass` 등을 적극 활용.
- **전용 스타터(Starter) 제공**:
    - 사용자가 복잡한 모듈 구성을 알 필요 없도록, 적절한 자동 설정 모듈들을 묶어주는 `my-library-starter`를 제공.
- **테스트 격리**:
    - 각 모듈은 자신이 책임지는 범위에 대해서만 독립적으로 테스트되어야 함. 모듈화가 잘 되어 있다면 테스트를 위해 무거운 전체 컨텍스트를
      띄울 필요가 없어짐.
