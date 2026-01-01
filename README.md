# 컴퓨터 공학 (Engineering)

단순한 호기심으로 일을 하는 것과 "전문가"로 일하는 방식은 구분해서 생각해야 한다.

1. 일상적인 고민, 일의 능률을 높이는 고민을 어떻게 코드로 혹은 AI의 도움으로 해결할지 고민한다.
2. 다른 사람들과 효율적으로 어떻게 협업할지 고민한다.

## Human side

대부분의 스포츠, 특히 육상에서는 개인전으로 성적을 측정한다.
그런 상황에서는 혼자 이기고 혼자 지게 된다.

하지만 팀 스포츠에서는 팀원들은 각자의 책임을 다해야 한다.
그래서 함께 이기고 함께 지는 법을 배우게 된다.

팀 스포츠는 "내가 최고의 선수가 되어야함" 책임이 있다.
내 팀원들이 나의 역할을 신뢰하기에 또한 나는 팀의 성과의 영향을 미칠 수 있기 때문에 팀의 성과를 위해 노력해야 한다.

그렇다면 팀의 정의는 뭘까?

단순히 이기는 걸까? 남 보다 더 잘하려고 하는게 목적인걸까?

나는 그것은 아니라고 생각한다.

팀은 비전을 공유하고 그것을 실현하기 위해 하나의 유기체가 되어야 한다.

---

역할의 분리로 "다른 것을 몰라야 한다는 것"은 아니다.
예를 들어, 프론트엔드와의 유의미한 대화를 하기 위해서는 그 세계의 언어를 이해해야 한다.

그러나 나는 백엔드 쪽과 아키텍처 쪽에 역할이 있다고 해서 프론트엔드 쪽을 모르고 넘어가는 것은 아니다.
이 말이 "모든 분야"에 대해서 알아야 한다는 것이 아니다.
"최대한 모든 각 유기체에 대한 이해를 하려는 시야를 갖자"를 뜻한다.

---

새로운 도구가 나오거나 나에게 새로우면 항상 다음의 질문으로 다가간다.

1. What is it?
2. How does it work?
3. Why does this exist?

---

## 사용하는 툴 (Tools)

- [MISE](https://github.com/jdx/mise): Multiple Runtime Version Manager
- [Termius](https://termius.com/index.html): SSH Client

## spring boot

- [common application properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html)
- [slice test](https://docs.spring.io/spring-boot/appendix/test-auto-configuration/slices.html)

---

## Gradle

### [difference between useJUnitJupiter and useJUnitPlatform](https://discuss.gradle.org/t/whats-the-difference-between-usejunitjupiter-and-usejunitplatform/43606)

#### `useJUnitPlatform()`: 엔진 실행 인프라 선택

- JUnit Platform을 실행기로 쓰겠다는 선언일 뿐
- 실제로 무엇을 실행할지는 테스트 엔진(Jupiter, Kotest, Spock 등)을 직접 추가해야 함

```kotlin
tasks.test {
  useJUnitPlatform()
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.1") //JUnit(Jupiter)

  testImplementation("io.kotest:kotest-runner-junit5:5.9.1") // kotest
}
```

#### `useJUnitJupiter()`: JUnit 테스트 프레임워크 선택

- JUnit(Jupiter)로 테스트를 작성·실행하겠다는 선언
- Gradle이 JUnit Platform + Jupiter 엔진 의존성까지 자동 구성

**JUnit Platform**은 “프레임워크”가 아니라 **엔진 실행 플랫폼**
