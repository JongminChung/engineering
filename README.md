# 컴퓨터 공학 (Engineering)

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
