# Java Modules (Project Jigsaw) & Module Import

Java 9에서 도입된 모듈 시스템(Project Jigsaw)은 자바 플랫폼의 구조를 근본적으로 개선하고, 대규모 애플리케이션의 캡슐화와
의존성 관리를 강화하기 위해 등장했습니다. 또한, Java 23에서는 이를 더욱 편리하게 사용할 수 있는 모듈 임포트 기능이 추가되었습니다.

---

## 1. Java 모듈 시스템 (Project Jigsaw)

### 등장 배경

- **Classpath 지옥(Jar Hell)**: 클래스패스에 중복된 클래스가 있거나 필요한 클래스가 없는 경우 런타임에 에러가 발생하는
  문제.
- **부족한 캡슐화**: `public` 클래스는 어디서든 접근 가능하여 내부 구현을 숨기기 어려웠음.
- **JVM 비대화**: `rt.jar`와 같은 거대 라이브러리로 인해 작은 디바이스에서도 전체 자바 런타임이 필요했음.

### 핵심 개념: `module-info.java`

모듈의 루트에 위치하며 해당 모듈의 명세(이름, 의존성, 공개 패키지 등)를 정의합니다.

- **requires**: 해당 모듈이 의존하는 다른 모듈을 지정합니다.
- **exports**: 다른 모듈에서 접근할 수 있도록 공개할 패키지를 지정합니다.
- **opens**: 리플렉션을 통한 접근을 허용할 패키지를 지정합니다.
- **provides ... with ...**: 서비스 제공자(Service Provider)를 정의합니다.
- **uses**: 모듈이 소비할 서비스를 지정합니다.

---

## 2. 모듈 시스템의 장점

1. **강력한 캡슐화**: `exports` 되지 않은 패키지는 외부에서 절대 접근할 수 없습니다. 심지어 리플렉션으로도 기본적으로
   차단됩니다.
2. **신뢰할 수 있는 구성**: 컴파일 타임 및 애플리케이션 시작 시점에 누락된 의존성을 확인하여 `NoClassDefFoundError`를
   조기에 방지합니다.
3. **확장성 및 경량화**: `jlink` 도구를 사용하여 애플리케이션 실행에 필요한 모듈만 포함하는 커스텀 런타임 이미지를 생성할 수
   있습니다.

---

## 3. Module Import Declarations (Java 23, JEP 476)

Java 23(Preview)에서 도입된 기능으로, 개별 클래스가 아닌 **모듈 전체를 임포트**할 수 있는 기능입니다.

### 주요 특징

- **편의성**: `import module java.base;` 한 줄로 해당 모듈이 익스포트하는 모든 public 클래스를 사용할 수
  있습니다.
- **가독성**: 수십 개의 개별 `import` 문을 획기적으로 줄여줍니다.
- **암시적 사용**: `java.base` 모듈은 모든 Java 프로그램에서 기본적으로 사용되므로, 향후 스크립팅이나 초보자 학습 시
  유용하게 활용됩니다.

### 코드 예시

```java
// Java 23 이전

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

// Java 23 이후 (Preview)
import module java.base;

public class ModuleImportExample {
	static void main(String[] args) {
		List<String> list = new ArrayList<>(); // java.util.List 자동 포함
		list.stream().toList(); // java.util.stream.Collectors 자동 포함
	}
}
```

---

## 4. 요약: 모듈화의 지향점

Java 모듈 시스템은 라이브러리와 프레임워크 개발자에게는 **강력한 경계 제어**를 제공하고, 애플리케이션 개발자에게는 **안전하고 효율적인
런타임 환경**을 제공합니다. Java 23의 모듈 임포트는 이러한 강력한 시스템을 보다 생산적으로 사용할 수 있게 돕는 사용자 친화적인
진화라고 볼 수 있습니다.
