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

클러스터에서 차트를 제거하려면 uninstall 명령을 실행한다.

```bash
helm uninstall pacman
```

## 템플릿 간 코드 공유

[\_helpers.tpl](templates/_helpers.tpl) 파일을 통해 템플릿 간 코드를 공유할 수 있다.

## 컨테이너 이미지 갱신

```bash
helm history pacman
```

```text
REVISION UPDATED                  STATUS   CHART        APP VERSION  DESCRIPTION
1        Tue Feb 03 01:54:28 2026 deployed pacman-0.1.0 1.0.0        Install complete
```

버전을 업데이트하려면 values.yaml을 열고 image.tag 필드를 최신 컨테이너 이미지 태그로 업데이트한다.

```bash
helm upgrade pacman .
```

```text
Release "pacman" has been upgraded. Happy Helming!
NAME: pacman
LAST DEPLOYED: Tue Feb 03 02:00:22 2026
NAMESPACE: default
STATUS: deployed
REVISION: 2
TEST SUITE: None
```

```bash
helm history pacman
```

```text
REVISION UPDATED                  STATUS   CHART        APP VERSION  DESCRIPTION
1        Tue Feb 03 01:54:28 2026 deployed pacman-0.1.0 1.0.0        Install complete
2        Tue Feb 03 02:00:22 2026 deployed pacman-0.1.0 1.0.1        Upgrade complete
```

--set 파라미터를 통해 속성의 값을 재정의하는 방법 뿐 아니라, YAML 파일을 통해서 재정의하느 방법도 제공한다.

`helm template pacman -f override.yaml`

## 헬름 차트 패키징 및 배포

`helm package .`

```text
Successfully packaged chart and saved it to: pacman-0.1.0.tgz
```

차트를 저장소에 게시하려면 index.yaml 파일을 새 메타데이터 정보로 업데이트하고 아티팩트를 업로드해야 한다.

```text
repo
-- index.yaml
-- pacman-0.1.0.tgz
```

```yaml
# index.yaml
apiVersion: v1
entries:
    pacman:
        - apiVersion: v2
          appVersion: 1.1.0
          created: "2026-02-03T02:06:25.528677+09:00"
          description: A Helm chart for Kubernetes
          digest: 20260203020625.528677+0900
          name: pacman
          type: application
          urls:
              - pacman-0.1.0.tgz
          version: 0.1.0
generated: "2026-02-03T02:06:25.528257+09:00"
```

`helm repo index .`를 실행하면 이 인덱스 파일을 자동으로 생성할 수 있다.

헬름은 패키징된 차트의 서명 파일을 생성하여 나중에 그 무결성을 검증할 수 있도록 하는 기능도 제공한다.
이 기능을 활용하면 차트가 임의로 수정되지 않은 올바른 차트인지 확인할 수 있다.

패키지를 서명하고 확인하려면 한 쌍의 GPG 키가 필요하다. 여기서는 이미 만들여져 있다고 가정하겠다.

이제 패키지 명령을 실행할 때 서명 파일 생성에 필요한 gpg 키를 전달하고 --sign 플래그를 지정하여 서명한다.

`helm package --sign --key 'me@example.com' --keyring ~./gnupg/secring.gpg .`

이 절차가 성공적으로 마무리되면 패키지 헬름 차트(.tgz)와 서명 파일(.tgz.prov)이 만들어진다.

```text
Chart.yaml
pacman-0.1.0.tgz
pacman-0.1.0.tgz.prov
```

차트가 유호하고 조작되지 않았는지 확인하려면 verify 명령어를 사용한다.

`helm verify pacman-0.1.0.tgz`

## 저장소에 보관된 차트 배포

`repo add` 명령을 사용하여 원격 저장소를 추가하고 `install` 명령을 사용하여 배포한다.

이를 위해 Bitnami와 같은 공개 저장소를 사용할 수 있다.

`helm repo add bitnami https://charts.bitnami.com/bitnami`

등록된 저장소를 나열하고 싶을 때는 다음과 같이 한다.

`helm repo list`

```text
NAME    URL
bitnami https://charts.bitnami.com/bitnami
```

PostgreSQL 차트를 배포하려면 `install` 명령을 실행하되, 차트의 위치를 로컬 디렉터리가 아닌
차트의 전체 이름(<repo>/<chart>)으로 변경한다.

```bash
helm install postgresql \
--set postgresql.postgresqlUsername=my-default,postgresql.postgresqlPassword=postgres,postgresql.postgresqlDatabase=my_db,postgresql.persistence.enabled=false \
bitnami/postgresql
```

```text
NAME: postgresql
LAST DEPLOYED: Tue Feb 03 02:22:32 2026
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
CHART NAME: postgresql
CHART VERSION: 11.1.4
APP VERSION: 17.1.0
```

서드 파티 차트를 이용하는 경우에는 그 기본값이나 재정의 가능 파라미터 목록을 직접 확인할 수는 없을 것이다.
헬름은 그런 값을 확인할 수 있도록 show 명령을 제공한다.

`helm show values bitnami/postgresql`

## 의존성을 가진 차트 배포
