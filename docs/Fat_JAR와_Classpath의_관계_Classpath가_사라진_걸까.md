# Fat JAR와 Classpath의 관계: "Classpath가 사라진 걸까?"

## 들어가며

Gradle로 Spring Boot 애플리케이션을 빌드하면 하나의 JAR 파일이 만들어지고, java -jar app.jar 한 줄로 실행됩니다. 예전처럼 -cp에 수십 개의 JAR를 나열하지 않아도 됩니다.

그렇다면 **Fat JAR가 되면서 classpath 자체가 사라진 걸까요?**

결론부터 말하면, **아닙니다.** classpath는 여전히 존재합니다. 다만 **그 범위와 구성 방식**이 달라졌을 뿐입니다. 이 글에서는 Fat JAR의 패키징 방식별로 classpath가 어떻게 동작하는지 정리합니다.

## 1. Classpath의 본질: 엔트리 단위는 `.class`가 아니다

많은 분들이 classpath를 "클래스 파일들의 경로"로 이해합니다. 틀린 말은 아니지만, 정확히는 **클래스 파일을 담고 있는 컨테이너의 경로**입니다.

classpath의 엔트리가 될 수 있는 대상은 **딱 두 가지**입니다.

| 엔트리 유형  | 예시                       |
| ------------ | -------------------------- |
| **디렉토리** | `build/classes/java/main/` |
| **JAR 파일** | `gson-2.10.jar`            |

> **"💡 개별 .class 파일은 classpath 엔트리가 될 수 없습니다."**

JVM이 클래스를 로딩하는 과정은 이렇습니다.

```text
"com.google.gson.Gson" 클래스를 찾아라!

→ classpath 엔트리를 순서대로 탐색:
   [0] my-app.jar        → com/google/gson/Gson.class 있나? ❌
   [1] gson-2.10.jar     → com/google/gson/Gson.class 있나? ✅ 발견!
   [2] commons-3.14.jar  → (탐색 중단)
```

classpath란 결국 **JVM이 클래스를 탐색할 JAR/디렉토리의 목록**입니다.

## 2. Before Fat JAR: classpath 엔트리가 N개

Fat JAR 이전에는 애플리케이션을 실행하려면 모든 의존성 JAR를 직접 나열해야 했습니다.

```bash
java -cp my-app.jar:gson-2.10.jar:commons-3.14.jar \
     com.myapp.Main
```

```text
classpath 엔트리 목록 (3개)
┌───────────────────────────────────────────────────┐
│ [0] my-app.jar                                    │
│      └── com/myapp/Main.class                     │
│                                                   │
│ [1] gson-2.10.jar                                 │
│      └── com/google/gson/Gson.class               │
│                                                   │
│ [2] commons-3.14.jar                              │
│      └── org/apache/commons/.../StringUtils.class │
└───────────────────────────────────────────────────┘
```

이 방식의 문제점은 명확합니다.

- **모든 JAR를 -cp에 나열**해야 합니다
- 진입점 클래스명도 직접 지정해야 합니다
- 배포 시 JAR 파일들의 상대 경로가 깨지기 쉽습니다

## 3. After Fat JAR: classpath 엔트리가 1개

Fat JAR는 이 문제를 해결합니다. 하지만 방식이 두 가지로 나뉩니다.

### 3-1. Shade/Shadow 방식: 풀어서 합치기

Gradle Shadow Plugin이나 Maven Shade Plugin이 사용하는 방식입니다.

```text
my-app-fat.jar
├── com/myapp/Main.class              ← 내 코드
├── com/google/gson/Gson.class        ← 의존성에서 꺼낸 .class
├── org/apache/commons/.../StringUtils.class
└── META-INF/MANIFEST.MF
```

모든 의존성 JAR를 **풀어서(unzip)** .class 파일들을 하나의 JAR에 합칩니다.

여기서 중요한 점은, **클래스 파일이 재생성(recompile)되는 것이 아니라는 점**입니다. 이미 컴파일된 .class 바이트코드를 **그대로 복사**합니다. 패키지 relocate 옵션을 사용할 경우에도 재컴파일이 아니라 바이트코드 내 패키지 참조 문자열만 치환합니다.

```kotlin
// Shadow Plugin의 relocate
shadowJar {
    relocate 'com.google.gson', 'shadow.com.google.gson'
    // → 바이트코드 내 문자열 치환일 뿐, 재컴파일이 아님
}
```

결국 JAR는 ZIP 포맷이므로, **여러 ZIP을 풀어서 하나의 ZIP으로 다시 묶는 것**이 Shade 방식의 본질입니다.

```text
gson-2.10.jar (ZIP)             my-app-fat.jar (ZIP)
├── com/google/gson/             ├── com/google/gson/
│   └── Gson.class       ──►     │   └── Gson.class        ← 복사
                                 ├── com/myapp/
commons-3.14.jar (ZIP)           │   └── Main.class        ← 복사
├── org/apache/commons/          ├── org/apache/commons/
│   └── StringUtils.class ──►    │   └── StringUtils.class ← 복사
                                 └── META-INF/MANIFEST.MF
```

### 3-2. Spring Boot 방식: JAR 안에 JAR 넣기

Spring Boot는 다른 전략을 취합니다. 의존성 JAR를 풀지 않고 그대로 내장합니다.

```text
my-app.jar
├── BOOT-INF/
│   ├── classes/                    ← 내 .class (원본 그대로)
│   │   └── com/myapp/Main.class
│   └── lib/                        ← 의존성 JAR (풀지 않고 그대로!)
│       ├── gson-2.10.jar
│       └── commons-lang3-3.14.jar
├── org/springframework/boot/loader/  ← 커스텀 클래스로더
└── META-INF/MANIFEST.MF
```

일반적으로 JVM은 "JAR 안의 JAR"를 읽을 수 없습니다. 그래서 Spring Boot는 자체 클래스로더(LaunchedURLClassLoader)를 사용하여 BOOT-INF/lib/ 안의 JAR들을 **런타임에 classpath로 등록**합니다.

## 4. `-jar` 옵션의 동작 원리

Fat JAR 실행의 핵심은 -jar 옵션입니다.

```bash
java -jar my-app.jar
```

이 명령이 실행되면 JVM은 다음과 같이 동작합니다.

1. `META-INF/MANIFEST.MF`에서 Main-Class를 읽는다
2. **해당 JAR 자체를 유일한 classpath 엔트리로 설정**한다
3. Main-Class의 main() 메서드를 실행한다

```text
# META-INF/MANIFEST.MF
Main-Class: com.myapp.Main
```

여기서 **반드시 알아야 할 것**이 하나 있습니다.

```bash
# ⚠️ -jar와 -cp를 동시에 사용하면 -cp는 무시됩니다!
java -cp some-other.jar -jar my-app-fat.jar
#    ^^^^^^^^^^^^^^^^^^^ 완전히 무시됨
```

-jar 옵션은 JVM에게 "이 JAR 하나만 바라봐라"고 선언하는 것과 같습니다. 이것이 Fat JAR가 "자체가 유일한 classpath 엔트리"가 되는 메커니즘입니다.

## 5. 전체 비교 정리

| 구분                | 실행 명령                   | classpath 엔트리 수 | 클래스 탐색 방식                  |
| ------------------- | --------------------------- | ------------------- | --------------------------------- |
| **개별 JAR**        | `java -cp a.jar:b.jar Main` | N개                 | JVM이 순차 탐색                   |
| **Shade Fat JAR**   | `java -jar fat.jar`         | 1개                 | JAR 내부 직접 탐색                |
| **Spring Boot JAR** | `java -jar app.jar`         | 1개 (논리적 N개)    | 커스텀 클래스로더가 내부 JAR 탐색 |

| 구분            | .class 처리 방식                      | 의존성 JAR 처리 |
| --------------- | ------------------------------------- | --------------- |
| **Shade**       | 바이트코드(.class) 복사 (재컴파일 ❌) | 풀어서 합침     |
| **Spring Boot** | 원본 그대로 재배치                    | JAR 째로 내장   |

## 마치며

Fat JAR가 되면서 사라진 것은 **classpath가 아니라, classpath를 직접 관리해야 하는 번거로움**입니다.

- **Shade 방식**은 모든 .class를 하나의 JAR로 합쳐서, JAR 자체가 유일한 classpath 엔트리가 됩니다
- **Spring Boot 방식**은 JAR 안에 JAR를 넣고, 커스텀 클래스로더가 런타임에 classpath를 구성합니다

어떤 방식이든 JVM 내부의 classpath 메커니즘은 여전히 동작하고 있습니다. 단지 그 구성이 **"개발자가 직접 나열"에서 "패키징 도구가 자동 구성"으로** 바뀌었을 뿐입니다.
