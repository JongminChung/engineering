# Marp

## 공식문서

- <https://marpit.marp.app/>

### 1. 기본 설정 (Front-matter)

파일의 최상단에 YAML 형식으로 설정을 추가합니다.

```markdown
---
marp: true
theme: default
paginate: true
backgroundColor: #fff
backgroundImage: url('https://marp.app/assets/hero-background.svg')
---
```

- `marp: true`: Marp 기능을 활성화합니다. (필수)
- `theme`: 사용할 테마 (`default`, `gaia`, `uncover` 등)
- `paginate`: 페이지 번호 표시 여부 (`true` / `false`)

### 2. 슬라이드 구분

슬라이드는 `---` (가로선)을 사용하여 구분합니다.

```markdown
# 첫 번째 슬라이드

내용입니다.

---

# 두 번째 슬라이드

다음 슬라이드 내용입니다.
```

### 3. 디렉티브 (Directives)

슬라이드 내에서 설정을 변경할 때 사용합니다.

- `<!-- _class: lead -->`: 현재 슬라이드에만 스타일 적용 (앞에 `_`가 붙으면 로컬 디렉티브)
- `<!-- class: invert -->`: 이후 모든 슬라이드에 스타일 적용 (글로벌 디렉티브)

### 4. 배경 이미지 및 레이아웃

```markdown
![bg right:40%](https://example.com/image.jpg)
```

- `bg`: 배경 이미지로 설정
- `left`, `right`: 분할 레이아웃 설정
- `opacity`: 투명도 조절

### 5. 텍스트 강조 및 스타일

- 일반 마크다운 문법을 그대로 사용합니다 (`#`, `**`, `-`, `1.` 등).
- HTML 태그를 사용하여 세부적인 스타일 조정이 가능합니다 (`<br>`, `<span>` 등).

### 6. 유용한 팁

- **수직 정렬**: 기본적으로 중앙 정렬이 아닐 경우 `_class: lead` 등을 사용하여 중앙 정렬을 유도할 수 있습니다.
- **이미지 크기 조절**: `![width:100px](image.png)`와 같이 마크다운 이미지 문법에 크기를 지정할 수 있습니다.
