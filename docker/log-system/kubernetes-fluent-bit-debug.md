# Kubernetes Fluent Bit 디버깅 가이드

이 문서는 `kubernetes-fluent-bit.yml` 기준으로 Fluent Bit 설정 적재와 로그 수집 흐름을 로컬에서 확인하는 방법을
정리합니다.

## 1) 배포 확인

```bash
kubectl get ns logging demo
kubectl -n logging get ds,pod
kubectl -n demo get deploy,pod
```

## 2) 설정(ConfigMap) 내용 확인

Fluent Bit 설정은 ConfigMap에 저장되어 DaemonSet으로 마운트됩니다. 가장 확실한 확인 방법은 ConfigMap을 직접
보는 것입니다.

```bash
kubectl -n logging get cm fluent-bit-config -o yaml
```

## 3) Pod 내부에서 파일 적재 확인

Fluent Bit 이미지에 `cat`이 없을 수 있으므로, 다음 순서로 확인합니다.

### 3-1. 쉘이 있는지 먼저 확인

```bash
kubectl -n logging exec -it <fluent-bit-pod> -- /bin/sh -c 'ls -la /fluent-bit/etc'
```

`/bin/sh` 자체가 없거나 `cat`, `sed` 같은 기본 유틸이 없다면 아래 3-2를 사용합니다.

### 3-2. Ephemeral Container로 파일 확인 (권장)

BusyBox를 임시 컨테이너로 붙여서 동일 볼륨을 확인합니다.

```bash
kubectl -n logging debug -it pod/<fluent-bit-pod> \
  --image=busybox:1.36 \
  --target=fluent-bit
```

연결된 셸에서:

```sh
ls -la /fluent-bit/etc
cat /fluent-bit/etc/fluent-bit.conf
cat /fluent-bit/etc/parsers.conf
```

## 4) 로그 수집 여부 확인

현재 OUTPUT이 `stdout`이므로 Fluent Bit 로그에서 수집 결과를 확인합니다.

```bash
kubectl -n logging logs -l app=fluent-bit -f
```

### 테스트 로그 생성 확인

`demo-logger`가 2초마다 로그를 찍습니다. 다음으로 앱 로그를 확인해 실제 로그가 생성되는지 봅니다.

```bash
kubectl -n demo logs -l app=demo-logger -f
```

Fluent Bit 로그에서 `demo-logger` 로그가 함께 보이면 수집 경로가 정상입니다.

## 5) 로그 경로 확인 (중요)

`kubernetes-fluent-bit.yml` 기준 INPUT 경로는 아래입니다.

```text
/var/log/pods/*/*/*.log
```

로컬 클러스터 환경(OrbStack/kind/minikube)에 따라 실제 로그 경로가 다를 수 있습니다.
경로가 다르면 Fluent Bit 로그가 비어 보일 수 있으니, 노드 경로를 먼저 확인하세요.

## 6) 문제 발생 시 체크리스트

- ConfigMap이 실제로 DaemonSet에 마운트되었는지 (`/fluent-bit/etc` 확인)
- Fluent Bit Pod가 Running 상태인지
- `/var/log` 호스트 경로가 존재하는지
- `demo-logger` 로그가 실제로 생성되는지
- Fluent Bit 로그에 에러가 있는지 (`kubectl -n logging logs <pod>`)
- Fluent Bit 로그에 `invalid pattern for given tag` 경고가 있는지
- Fluent Bit 로그가 자기 자신을 다시 읽는지(로그 폭증) 확인하고 필요 시 `Exclude_Path` 적용

## 7) OrbStack(Docker 런타임) 환경에서 자주 발생하는 이슈

OrbStack이 Docker 런타임(`docker://...`)을 쓰는 경우 로그 파일 포맷이 Docker JSON일 수 있습니다.
현재 설정은 `Parser cri`라서 JSON 로그를 파싱하지 못하고, Fluent Bit 로그에 레코드가 출력되지 않을 수 있습니다.

### 실제 로그 포맷 확인

```bash
kubectl get nodes -o wide
kubectl debug node/orbstack -it --image=busybox:1.36 -- chroot /host \
  sh -c 'head -n 3 /var/log/pods/<ns>_<pod>_<uid>/<container>/0.log'
```

출력이 아래처럼 JSON이면 `cri`가 아니라 `docker` 파서를 써야 합니다.

```text
{"log":"2026-01-03T08:38:31+00:00 demo-logger hello\n","stream":"stdout","time":"2026-01-03T08:38:31.517414134Z"}
```

### symlink 경로 확인 (Docker 런타임)

`/var/log/pods/.../0.log`가 다음처럼 `/var/lib/docker/containers/...`로 연결될 수 있습니다.

```bash
kubectl -n logging debug pod/<fluent-bit-pod> --copy-to=fluent-bit-debug \
  --container=fluent-bit --image=busybox:1.36 -- \
  sh -c 'ls -la /var/log/pods/<ns>_<pod>_<uid>/<container>'
```

출력이 아래처럼 나오면 Fluent Bit에도 `/var/lib/docker/containers` 마운트가 필요합니다.

```text
0.log -> /var/lib/docker/containers/<id>/<id>-json.log
```

### 대응 방법(요약)

- `parsers.conf`에 `docker` 파서를 추가하거나 기본 파서를 `docker`로 변경
- `fluent-bit.conf`의 INPUT `Parser`를 `docker`로 변경
- 변경 후 `kubectl -n logging logs -l app=fluent-bit -f`로 수집 로그 확인
- `/var/log/pods/.../0.log`가 symlink인 경우 `Docker_Mode On`과
  `Docker_Mode_Parser docker` 사용
- `Follow_Symlinks`는 v2.2.3 tail 입력에서 지원되지 않아 에러가 발생함
- `0.log`가 `/var/lib/docker/containers`를 가리키면 해당 경로를 DaemonSet에 마운트해야 함
- `/var/log/pods` 경로 사용 시 `Regex_Parser`를 추가해 태그 파싱 경고를 제거
- Fluent Bit 로그가 자기 자신을 읽는 경우 `Exclude_Path`로 `logging/fluent-bit` 로그를 제외

## 8) 왜 로그가 안 쌓였는지(원인 분석)

이번 환경에서 로그가 안 보였던 이유는 여러 단계가 겹쳤기 때문입니다.

1. 로그 포맷 불일치
   OrbStack가 Docker 런타임을 사용하면서 로그가 Docker JSON 형태로 기록됨.
   기존 `Parser cri` 설정은 JSON을 파싱하지 못해 레코드가 출력되지 않음.

2. symlink 경로 문제
   `/var/log/pods/.../0.log`가 실제로는 `/var/lib/docker/containers/...-json.log`로
   연결됨.
   Fluent Bit 컨테이너에 `/var/lib/docker/containers` 마운트가 없어서 파일을 읽지 못함.

3. 태그 파싱 불일치
   `/var/log/pods` 경로 기반 태그가 기본 패턴과 달라 `invalid pattern for given tag` 경고 발생.
   Kubernetes 메타데이터 부여가 제대로 되지 않음.

4. 자기 로그 재수집 루프
   Fluent Bit 로그를 다시 읽어 로그가 폭증하고 실제 앱 로그를 찾기 어려움.

위 문제를 순서대로 해결하면서 정상 수집이 확인되었습니다.

## 9) containerd(표준 런타임) 환경 안내

운영 환경에서 `containerd`를 사용하는 경우 로그 포맷은 보통 CRI 형식이며, 다음 설정 조합이 일반적입니다.

- INPUT `Parser`는 `cri`
- `Docker_Mode`는 사용하지 않음
- `/var/log/pods`와 `/var/log/containers` 경로가 정상적으로 존재

환경별로 경로가 다를 수 있으므로, 노드에서 실제 파일 경로를 확인하는 것이 가장 확실합니다.

## 10) node-debugger 개념 정리

`kubectl debug node/<node>`를 실행하면 해당 노드에 디버그용 파드가 생성됩니다.
이 파드는 일반적으로 `node-debugger-<node>-<suffix>` 형태로 보이며, 노드 파일시스템을
`/host`로 마운트해 `chroot /host` 기반 점검을 가능하게 합니다.

### 특징과 사용 의도

- 노드 수준의 파일/로그 경로 확인을 위한 임시 파드입니다.
- 디버깅이 끝나면 수동으로 삭제해야 남지 않습니다.
- 동일 노드에 여러 번 생성하면 파드가 누적될 수 있습니다.

### 삭제 방법

```bash
kubectl -n default delete pod node-debugger-<node>-<suffix>
```

라벨이 없다면 이름으로 삭제하는 방식이 가장 확실합니다.

## 11) 변경 후 실제 동작 검증 절차(상세)

### 8-1. 변경 적용

```bash
kubectl apply -f kubernetes-fluent-bit.yml
```

### 8-2. Fluent Bit 재기동 확인

```bash
kubectl -n logging rollout status ds/fluent-bit
kubectl -n logging get pod -l app=fluent-bit
```

### 8-3. 설정 적재 확인 (Ephemeral Container)

```bash
kubectl -n logging debug -it pod/<fluent-bit-pod> \
  --image=busybox:1.36 \
  --target=fluent-bit
```

셸에서 다음을 확인합니다.

```sh
cat /fluent-bit/etc/fluent-bit.conf
cat /fluent-bit/etc/parsers.conf
```

`Parser docker`가 들어가 있는지 확인합니다.

### 8-4. 수집 로그 확인

```bash
kubectl -n logging logs -l app=fluent-bit -f
```

`demo-logger` 로그가 안 나오면 `Docker_Mode On`, `Docker_Mode_Parser docker`,
`Read_from_Head On`이 들어갔는지 확인합니다.
`fluent-bit` 로그만 보이면 `Exclude_Path`가 적용됐는지 확인합니다.

### 8-5. 노드 로그 포맷 재확인 (옵션)

```bash
kubectl debug node/orbstack -it --image=busybox:1.36 -- chroot /host \
  sh -c 'head -n 3 /var/log/pods/<ns>_<pod>_<uid>/<container>/0.log'
```

JSON 로그가 유지된다면 `docker` 파서가 맞습니다.
