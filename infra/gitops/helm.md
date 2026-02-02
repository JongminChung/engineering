# Helm

커스터마이즈와 헬름의 차이점 하나는 차트(chart) 개념이다.
차트는 공유 가능한 쿠버네티스 패키지로, 다른 차트에 대한 의존성 등 다양한 요소를 포함한다.

애플리케이션 설정값은 일반적으로 쿠버네티스 ConfigMap에 대응되는 속성이다.
ConfigMap은 수정되어도 애플리케이션의 롤링 업데이트로 이어지지 않으므로,
수동으로 다시 시작할 때까지 애플리케이션은 이전 버전으로 실행된다.

헬름은 애플리케이션의 ConfigMap이 변경되면 자동으로 롤링 업데이트가 시작되도록
하는 기능 몇 가지를 제공한다.

---

## 디렉터리

```text
.
├── Chart.yml
├── values.yml
└── templates/
    ├── deploy.yml
    └── svc.yml
```

- Chart.yml: 차트를 기술하며(descriptor) 차트 관련 메타데이터를 포함한다.
- templates/: 애플리케이션을 배포하는 Kubernetes 리소스 정의 파일들이 위치한다.
- values.yml: 애플리케이션 설정값을 정의한다.

---

헬름 차트를 로컬에서 YAML으로 렌더링해 보려면 터미널 창에서 다음 명령을 실행하면 된다.

```bash
helm template .
```

헬름 명령어의 `--set` 파라미터를 사용하면 기본값을 재정의할 수 있다.
`replicaCount` 값을 1(`values.yaml`에 정의된 값)에서 3으로 재정의해 보겠다.

```bash
helm template --set replicaCount=3 .
```

## 배포

```bash
helm install .
```

```text
NAME: pacman
LAST DEPLOYED: Tue Feb 03 01:54:28 2026
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
```

history 명령어를 사용하면 배포된 헬름 차트의 이력 정보도 얻을 수 있다.

```bash
helm history pacman
```

```text
REVISION	UPDATED                 	STATUS  	CHART       	APP VERSION	    DESCRIPTION
1       	Tue Feb 03 01:54:28 2026	deployed	pacman-0.1.0	1.0.0        	Install complete
```
