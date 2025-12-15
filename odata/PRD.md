PRD: OData Library — v1 초점(Filtering, Ordering) & Spring RestController 자동 바인딩

1. 목적

- Apache Olingo(OData v4) 기반의 내부용 OData 클라이언트를 구축한다.
- 1차 목표는 Spring의 `@RestController`에서 목록 조회(Read-only) 시나리오에 필요한 `$filter`와 `$orderby`를 안정적으로 지원하고,
  쿼리 문자열을 컨트롤러 시그니처의 `Filter<T>`, `OrderBy<T>` 타입으로 자동 파싱/바인딩하는 것이다.
- 이후 확장(OSS 전환 포함)을 고려한 모듈형/확장 가능한 설계를 유지한다.

1. 범위

- v0: 스캐폴드(모듈/빌드/컨벤션), PRD/설계 문서 정리(코드 구현 전 단계)
- v1: 읽기 전용 쿼리 중심 기능
    - 지원: `$filter`, `$orderby`
    - 비지원(차기): `$select`, `$expand`, `$top`, `$skip`, CRUD, 리트라이/회로차단기, 고급 메타데이터 기능
- v2+: `$select/$expand/$top/$skip`, CRUD, 배치/대량 연산, 변경 추적(delta), 캐싱, 고급 회복력, 공개 확장 포인트

1. 사용/가정(현 단계)

- 사용 맥락: Spring `@RestController`에서 HTTP 쿼리 파라미터를 받아 OData 서비스로 위임하는 BFF/프록시 형태
    - 쿼리 파라미터 키: `filter`, `orderBy` (케이스 고정)
    - 컨트롤러 파라미터 타입: `Filter<T>`, `OrderBy<T>` — 문자열을 자동으로 도메인 제네릭 타입의 값 오브젝트로 변환
    - 페이징: 서버별 제약 상이하므로 v1에서는 라이브러리 차원의 강제 미적용(컨트롤러에서 별도 처리)
- 보안: v1은 간단한 Bearer 토큰/고정 헤더 주입 정도만 지원(확장 포인트로 노출)

1. 요구사항(상세)

- 구성
    - `ODataClientConfig`: `baseUrl`, 인증 헤더 전략, 연결/읽기 타임아웃, 기본 헤더, 로깅 레벨
    - 인터셉터: 요청/응답 로깅, 헤더 주입용 간단 인터페이스
- 쿼리 기능
    - `$filter` 지원 연산자(우선): 비교 `eq/ne/gt/ge/lt/le`, 논리 `and/or/not`, 문자열 `startswith/endswith/contains`, null 체크, 괄호 그룹핑
    - 타입 매핑: 문자열 `'value'`, 숫자/불리언 리터럴, 날짜/시간(ISO-8601 문자열) — v1은 단순 포맷팅만 지원
    - `$orderby`: `field asc|desc` 목록
    - 인코딩: 안전한 URL 인코딩 처리(공백, 특수문자)
- Spring 자동 바인딩
    - 컨버터: `String -> Filter<T>`, `String -> OrderBy<T>` 제네릭 컨버터 제공(타겟 제네릭 타입 보존)
    - 설정: `ODataWebMvcConfigurer`가 Spring `FormattingConversionService`에 컨버터 등록
    - 화이트리스트: `PropertyWhitelist`(선택)로 타입별 허용 필드를 검증하여 필터/정렬 인젝션 리스크를 경감
- 오류 처리
    - 파서 예외 `ODataParseException` 제공(오프셋/토큰 정보 포함), 컨트롤러에서 400으로 매핑하는 예시 제공
- 성능/안정성
    - 단순 GET 전송, 커넥션 풀/타임아웃 설정 가능

1. 아키텍처(경량)

- 모듈 구성(설계):
    - v1 개발 시 실제 구현은 두 레이어로 분리
        - `odata-core`: 파서/AST/빌더/직렬화(순수 Java, Spring 의존성 없음)
        - `odata-spring`: Spring MVC 제네릭 컨버터와 설정(`ODataWebMvcConfigurer`), 필드 화이트리스트 인터페이스
    - 현재 레포는 `odata` 단일 모듈만 존재하며, 구현 단계에서 위 2개 서브모듈로 분리 예정
- 핵심 컴포넌트
    - `ODataClientConfig`: 구성 보관
    - `ODataClient`: 엔드포인트 호출 책임, `entitySet(name)`로 시작
    - `EntitySetQuery`: `filter(Filter<?>|String)`, `orderBy(OrderBy<?>|String)`, `execute(Class<T>)`
    - `Filter`/`OrderBy` 모델 & 빌더: 타입-세이프 헬퍼와 문자열 파스스루 병행 지원
    - `Interceptors`: 요청/응답 로깅, 헤더 주입
- Spring 통합
    - `String -> Filter<T>` / `String -> OrderBy<T>` `GenericConverter`
    - `PropertyWhitelist`로 필드 검증(선택)

1. 공개 API 스케치(컨트롤러 친화)

```java
// Controller 내부 예시 — 문자열이 자동으로 파싱되어 바인딩됨
@GetMapping("/products")
public List<Product> list(
        @RequestParam(required = false) Filter<Product> filter,
        @RequestParam(required = false, name = "orderBy") OrderBy<Product> orderBy
) {
    return client.entitySet("Products")
            .filter(filter)        // 이미 파싱된 모델 사용
            .orderBy(orderBy)
            .execute(Product.class);
}

// 빌더 사용 예 — 서버 사이드에서 안전하게 생성 가능
Filter<Product> built = Filter.and(
        Filter.eq("Category", "Beverages"),
        Filter.gt("UnitPrice", 20)
);

OrderBy<Product> ob = OrderBy.of(
        OrderBy.asc("Category"),
        OrderBy.desc("UnitPrice")
);
```

1. 테스트 전략

- 단위 테스트
    - 파서/토크나이저: 리터럴, 이스케이프, 괄호/우선순위(`not`>`and`>`or`), 함수 호출(`startswith/endswith/contains`)
    - 직렬화: AST ↔ 문자열 round-trip, 안전한 인코딩 위임 확인
    - Filter/OrderBy 빌더: 표현식 생성, 괄호/연산자 우선순위
    - Spring 컨버터: `DefaultFormattingConversionService`로 `String->Filter<T>`/`OrderBy<T>` 변환 테스트
- 통합 테스트
    - 공개 OData v4 데모(예: OData v4 Demo Service) 대상 read-only 호출로 필터/정렬 검증

1. 수용 기준(AC)

- PRD/README 공개 및 빌드 성공
- `$filter`, `$orderby` 표현을 정확히 생성하고 URL 인코딩 처리를 상위 레이어에 위임(직렬화 문자열은 스펙에 맞음)
- Spring Controller 예제에서 `Filter<T>`, `OrderBy<T>` 자동 바인딩이 동작(컨버터 테스트로 검증)
- 통합 테스트에서 필터/정렬 결과가 기대와 일치

1. 버저닝/배포

- SemVer: v0.x(내부), v1.0.0(안정)
- 내부 배포 이후 OSS 전환(라이선스/문서/예제 포함)

1. 마일스톤

- M1: PRD 업데이트(본 문서) 및 리뷰 승인
- M2: 코어 구현(`odata-core`): 파서/AST/직렬화 + 빌더 + 단위 테스트
- M3: Spring 통합(`odata-spring`): 제네릭 컨버터/설정 + 컨버터 테스트 + 예제 스니펫
- M4: `ODataClient`/`EntitySetQuery` 최소 기능 + 공개 데모 서비스 연동 통합 테스트(읽기 전용)
- M5: README(컨트롤러 예제, 지원 문법, 화이트리스트 가이드)

1. 리스크

- 공급사별 OData 방언 및 구현 차이: v1 범위 내에서 표준 중심 최소 표현만 지원
- 필터 문자열 인젝션 위험: 빌더 사용 권장, raw 문자열 사용 시 밸리데이션/화이트리스트 옵션 제공(차기 강화)
- Olingo 의존성 변경 리스크: 버전 고정 및 릴리스 모니터링

1. 승인 요청

- 본 PRD는 v1 범위(Filtering/Ordering, Spring RestController 가정)에 맞춰 업데이트되었음.
- 승인 시, 설계에 따라 최소 기능 구현을 진행한다.
