# Code CLI

_**Table of Contents**_

<!-- @formatter:off -->
<!-- TOC -->

- [Code CLI](#code-cli)
    - [SDLC (Software Development Life Cycle)](#sdlc-software-development-life-cycle)
    - [CLI](#cli)
    - [rules](#rules)
    - [AGENTS.md](#agentsmd)
    - [Custom Prompts](#custom-prompts)
    - [Skills](#skills)
          <!-- TOC -->
          <!-- @formatter:on -->
          <!-- markdownlint-enable -->

* 요청의 성공 여부를 검증할 수 있도록 합니다.
    - 문제를 제공, 린팅, 검증 단계를 제공합니다.

* 복잡한 작업을 작고 집중된 단계로 나눠야 합니다.
    - 작업을 어떻게 나눠야 할지 잘 모르겠다면, CLI로 계획을 제안해 달라고 요청합니다.

---

## SDLC (Software Development Life Cycle)

Building an AI-Native Engineering Team에 대한 Codex CLI팀의 문서[^1]입니다.

**1. 역할의 근본적인 변화: Delegate(위임) → Review(검토) → Own(소유)**

엔지니어는 이제 기계적인 작업을 AI에게 **위임**하고, 결과물을 **검토**하며, 최종적인 방향성과 품질을 **소유**하는 방식으로
일합니다.

- 위임 (Delegate): 반복적인 보일러플레이트 코드 작성, 초기 설계안 도출, 로그 분석, 테스트 케이스 생성 등 기계적이고 시간이 많이
  걸리는 작업을 AI 에이전트에게 맡깁니다.
- 검토 (Review): AI가 생성한 코드의 아키텍처 적합성, 보안, 성능, 그리고 비즈니스 로직의 정확성을 세밀하게 확인합니다.
- 소유 (Own): 시스템의 장기적인 유지보수성, 복잡한 아키텍처 결정, 제품의 의도 설정 및 최종 배포에 대한 책임은 여전히 엔지니어에게
  있습니다.

**2. 단계별 주요 작업 방식**

| 단계              | 엔지니어의 새로운 작업 방식                                                                                                       |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| 기획 (Plan)       | AI가 분석한 코드베이스 기반의 타당성 조사를 검토하고, 우선순위 설정 및 전략적 의사결정에 집중합니다.                              |
| 설계 (Design)     | AI가 초기 프로토타입과 UI 스터브를 생성하는 동안, 엔지니어는 핵심 로직과 확장 가능한 아키텍처 패턴을 구축합니다.                  |
| 개발 (Build)      | AI가 전체 기능을 구현(API, UI, 데이터 모델 등)하면, 엔지니어는 비즈니스 로직의 특이 케이스를 정제하고 코드의 일관성을 확인합니다. |
| 테스트 (Test)     | AI가 제안한 엣지 케이스를 비판적으로 검토하고, 테스트가 기능 사양 및 사용자 경험과 일치하는지 확인합니다.                         |
| 검토 (Review)     | AI가 일차적으로 잡은 버그 수정을 바탕으로, 코드의 구조적 정렬과 컨벤션 준수 여부를 최종 승인합니다.                               |
| 문서화 (Document) | AI가 요약한 문서에 '왜(Why)' 이러한 결정을 내렸는지에 대한 맥락을 추가하고 전체적인 구조를 관리합니다.                            |
| 운영 (Maintain)   | AI가 로그를 분석하고 원인을 추적하면, 엔지니어는 제안된 해결책을 검증하고 복원력 있는 수정안을 설계합니다.                        |

**3. 엔지니어가 갖춰야 할 새로운 접근법**

- 프롬프트 및 가이드라인 관리: AGENTS.md 파일 등을 통해 에이전트가 따를 테스트 커버리지 기준이나 문서화 규칙을 설정하고 관리해야
  합니다.
- 도구 통합 (MCP 활용): AI 에이전트가 이슈 트래커, 로깅 도구, 디자인 시스템 등에 접근할 수 있도록 인프라를 연결하고 권한을
  설정하는 역할을 수행합니다.
- 고차원적 사고에 집중: 단순 구현에서 벗어나 시스템 수준의 추론, 새로운 추상화 설계, 모호한 요구사항의 명확화 등 인간의 직관이 필요한
  복잡한 과제에 더 많은 시간을 할애해야 합니다.

결론적으로, 현대의 엔지니어는 **"한 줄씩 코드를 짜는 장인"에서 "AI라는 숙련된 조수들을 거느리고 전체 오케스트라를 지휘하는 지휘자"**
로 변모하고 있습니다. AI가 악보의 음표(코드)를 채우는 동안, 엔지니어는 곡의 해석(아키텍처)과 감동(사용자 가치)을 책임지는
역할을 맡게 됩니다.

---

## CLI

1. 대화를 지속할 수 있다. (resume)
2. 이미지를 입력으로 넣을 수 있다. (`codex -i img1.png,img2.png`)
3. 코드 MR을 Open하기 이전에 `review`를 통해서 셀프 리뷰를 할 수 있다.
4. `codex --cd apps/fronted --add-dir ../backend --add-dir ../shared`도 가능하다.

```toml
model = "gpt-5.2-codex"
model_reasoning_effort = "medium"

[profiles.fa]
approval_policy = "never"
sandbox_mode = "danger-full-access"
```

```bash
# .zshrc
alias rm='rm -i'
```

---

## rules

```text
# ~/.codex/rules/rm.rules

pattern = ["rm"]
decision = "prompt"
justification = "Deletion of files always requires confirmation"

# 검증
codex execpolicy check --rules ~/.codex/rules/rm.rules --pretty -- rm -rf build
```

---

## AGENTS.md

최대한 직접적으로 Codex CLI나 CLAUDE에게 직접 질의해서 사용한다.

---

## Custom Prompts

`~/.codex/prompts/add-tests.md`

```markdown
---
description: Add meaningful tests for changes
---

Add tests for the referenced code.

Scope:

- Target module: mention files

Priority:

1. Unit tests for business logic
2. Integration tests only if behavior crosses DB/queue

Constraints:

- No real network calls
- Prefer Testcontainers when needed

Output:

If target is java code:

- Place tests under same package structure of the code
- Use JUnit Jupiter + AssertJ + Testcontainers

If target is typescript code:

- Place tests under same package structure of the code
- Use Jest + Testcontainers

If tests are not feasible:

- Explain why and suggest a lightweight alternative
```

---

## Skills

TODO

---

[^1]: <https://developers.openai.com/codex/guides/build-ai-native-engineering-team>
