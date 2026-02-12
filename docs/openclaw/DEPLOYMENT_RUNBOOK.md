# OpenClaw Idempotent Runbook (CentOS 9 + Podman Compose)

Mattermost가 이미 설치된 서버(`/root/mattermost/docker`)를 기준으로, OpenClaw를 멱등하게 설치/재적용하는 절차입니다.

## 0. 범위

- 포함: OpenClaw 소스 설치/갱신, 이미지 빌드, Gateway 기동, OAuth 준비, 로컬 접근
- 제외: Mattermost 자체 설치/업그레이드

## 1. 기본 원칙

- Compose 실행은 `podman compose`만 사용
- 외부 노출 없이 `127.0.0.1:18789/18790` 로컬 바인딩
- 재실행해도 같은 상태를 보장(멱등)
- 실행 진입점은 Python 스크립트 하나로 통일

## 2. 스크립트 위치

서버에서 OpenClaw 소스를 `/root/openclaw`에 두고 아래 스크립트를 사용한다.

- `/root/openclaw/docs/openclaw/bootstrap-openclaw.py`

## 3. 전체 멱등 실행 (권장)

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py'
```

## 4. 단계별 실행 (필요 시)

### 4.1 Compose provider 정리

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py --only-step compose'
```

### 4.2 소스 동기화

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py --only-step repo'
```

### 4.3 `.env`/state 구성

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py --only-step env'
```

### 4.4 이미지 빌드 + Gateway 기동

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py --only-step build'
```

### 4.5 `gateway.mode=local` 보정

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py --only-step onboard-fix'
```

### 4.6 Mattermost 플러그인 중복 정리

원칙: 기본 포함본(`/app/extensions/mattermost`) 유지 + 수동 설치본 제거

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py --only-step plugin'
```

### 4.7 상태 점검

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py --only-step verify'
```

## 5. OAuth 수동 처리 포인트

OAuth는 운영자가 수동으로 수행한다.

```bash
ssh openclaw 'cd /root/openclaw && podman compose run --rm openclaw-cli onboard --no-install-daemon'
```

진행 시 선택:

- Auth: `openai-codex`
- OAuth(PKCE) 인증은 브라우저에서 수동 완료

완료 확인:

```bash
ssh openclaw 'cd /root/openclaw && podman compose run --rm openclaw-cli models status'
```

## 6. 로컬 PC에서 OpenClaw 접근

터널 생성:

```bash
ssh -N -L 18789:127.0.0.1:18789 openclaw
```

브라우저 접속:

- `http://localhost:18789/`
- 토큰 URL은 아래 명령 출력값 사용

```bash
ssh openclaw 'cd /root/openclaw && podman compose run --rm openclaw-cli dashboard --no-open'
```

## 7. 재실행 가이드

문제 발생 시 우선 아래 하나만 실행:

```bash
ssh openclaw 'python3 /root/openclaw/docs/openclaw/bootstrap-openclaw.py'
```
