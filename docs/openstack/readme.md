# OpenStack

단일 노드 Keystone + Swift + Horizon 으로

- s3 호환 API 가능 여부 테스트
- swift api 테스트
- keystone (accessKey, secretKey) 어떻게 관리하고 이를 root, iam 계정 어떻게 적용할지에 대해서 확인하려고
	함.

## Index

- [개념 및 공식 문서 링크](#개념-및-공식-문서-링크)
- [로컬 PoC 환경 옵션](#로컬-poc-환경-옵션)
- [PoC 절차: OrbStack + Rocky 9 + Kolla-Ansible](#poc-절차-orbstack--rocky-9--kolla-ansible)
- [PoC 검증](#poc-검증)
- [트러블슈팅](#트러블슈팅)
- [설치 진행 모니터링](#설치-진행-모니터링)
- [실패 흔적 정리](#실패-흔적-정리)

## 개념 및 공식 문서 링크

- Keystone(Identity): <https://docs.openstack.org/keystone/latest/>
- Swift(Object Storage): <https://docs.openstack.org/swift/latest/>
- 인증 토큰/카탈로그
	개념: <https://docs.openstack.org/keystone/latest/getting-started/identity-concepts.html>
- OpenStackClient(
	CLI): <https://docs.openstack.org/python-openstackclient/latest/>
- SwiftClient(CLI): <https://docs.openstack.org/python-swiftclient/latest/>
- Kolla-Ansible(컨테이너 배포): <https://docs.openstack.org/kolla-ansible/latest/>

## 로컬 PoC 환경 옵션

- VM 추천(Mac 기준): OrbStack에 Rocky 9 amd64 VM 생성 후 Kolla-Ansible 설치함
 	- 단일 노드, 컨테이너 기반, 설치/삭제 간단함
 	- Keystone/Swift PoC 목적에 충분함

## PoC 절차: OrbStack + Rocky 9 + Kolla-Ansible

### 1) Rocky 9 VM 생성(amd64)

- OrbStack에서 Rocky 9 VM 생성함
- OrbStack VM 가이드: <https://docs.orbstack.dev/machines/>
- 최소 사양: 4 CPU / 8GB RAM / 40GB 디스크 권장함
- `ssh orb`로 VM 접속 가능함

### 2) 기본 패키지 및 Docker Engine 설치

```bash
sudo dnf -y install epel-release
sudo /usr/bin/crb enable
sudo dnf -y install python3 python3-pip git which ncurses

# podman-docker가 설치되어 있으면 제거함
sudo dnf -y remove podman-docker || true

# Docker CE repo 추가 후 설치함
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf -y install docker-ce docker-ce-cli containerd.io
sudo systemctl enable --now docker
```

```bash
# 일반 사용자로 docker 명령 실행 시 권한 오류가 나면 그룹 추가함
sudo usermod -aG docker $USER
newgrp docker

docker ps
```

```bash
# kolla-ansible prechecks에서 docker SDK 누락 시 설치함
sudo /usr/local/bin/pip install docker
```

### 2-1) IPv6 모듈 활성화(필수)

- `kolla-ansible deploy` 중 IPv6 sysctl 적용 실패를 피하려고 먼저 적용함

```bash
sudo modprobe ipv6
sudo sh -c 'echo ipv6 > /etc/modules-load.d/ipv6.conf'
sudo sysctl -w net.ipv6.conf.all.disable_ipv6=0
sudo sysctl -w net.ipv6.conf.default.disable_ipv6=0
```

### 3) Kolla-Ansible 설치

```bash
sudo sh -c 'echo "export PATH=/usr/local/bin:$PATH" > /etc/profile.d/kolla-path.sh'
source /etc/profile.d/kolla-path.sh

sudo python3 -m pip install -U pip
sudo /usr/local/bin/pip install 'ansible>=6,<9' kolla-ansible
```

### 4) Kolla-Ansible 초기 설정

```bash
sudo mkdir -p /etc/kolla
sudo chown -R $USER:$USER /etc/kolla

# 의존성 설치
sudo sed -i "s/stable\\/2024.1/unmaintained\\/2024.1/" /usr/local/share/kolla-ansible/requirements.yml
kolla-ansible install-deps

# 샘플 설정 및 인벤토리 복사
cp -r /usr/local/share/kolla-ansible/etc_examples/kolla/* /etc/kolla
cp /usr/local/share/kolla-ansible/ansible/inventory/all-in-one ~/all-in-one
```

- `/etc/kolla/globals.yml`에 아래 항목을 추가함
- `kolla_internal_vip_address`는 같은 서브넷의 사용하지 않는 IP로 지정함
 	- 예: VM이 `192.168.139.30/24`이면 `192.168.139.250` 같은 빈 IP로 지정함
 	- VM의 실제 IP(`eth0`)와 동일한 주소는 사용하면 안 됨
 	- 샘플 파일이 없으면 아래 경로에서 복사 후 편집함
  		- `/usr/local/share/kolla-ansible/etc_examples/kolla/globals.yml`

```yaml
kolla_tag: "2024.1-rocky-9"
docker_registry: "quay.io"
docker_namespace: "openstack.kolla"
kolla_base_distro: "rocky"
kolla_install_type: "binary"
network_interface: "eth0"
neutron_external_interface: "eth0"
kolla_internal_vip_address: "192.168.139.250"
enable_keystone: "yes"
enable_horizon: "yes"
enable_swift: "yes"
enable_swift_s3api: "yes"
swift_account_server_port: 6002
swift_container_server_port: 6001
enable_nova: "no"
enable_neutron: "no"
enable_glance: "no"
enable_heat: "no"
enable_placement: "no"
enable_openvswitch: "no"
```

- Swift 포트 오버라이드 포함함. 상세는 [Swift 503 (포트 뒤바뀜)](#swift-503-포트-뒤바뀜) 참고함

```bash
ip -o -4 addr show | awk '{print $2, $4}' # kolla_internal_vip_address eth0 인터페이스 중 사용하지 않는 IP
```

**kolla-genpwd는 Kolla에서 쓰는 서비스 계정/DB/RabbitMQ 등의 기본 비밀번호를 한 번에 생성해서
/etc/kolla/passwords.yml에 저장하는 명령**

- 역할: 모든 OpenStack 서비스 비밀번호를 랜덤으로 생성
- 위치: /etc/kolla/passwords.yml
- 필수: kolla-ansible deploy 전에 반드시 한 번 실행해야 함

```bash
kolla-genpwd
```

### 5) Swift ring 파일 생성

```bash
sudo mkdir -p /etc/kolla/config/swift /srv/node/sdb
sudo mkdir -p /etc/kolla/config/swift/backups
sudo chmod 777 /etc/kolla/config/swift /etc/kolla/config/swift/backups
```

```bash
SWIFT_IMAGE=quay.io/openstack.kolla/swift-object:2024.1-rocky-9
sudo docker pull "$SWIFT_IMAGE"
```

- 로컬 레지스트리 사용 시 `SWIFT_IMAGE`를 동일한 registry/tag로 교체함
- `SWIFT_IP`는 VM의 `eth0` 기본 IP로 맞춰야 함
 	- 보조 IP까지 같이 나오면 첫 번째 IPv4만 쓰도록 제한해야 함
 	- 다른 IP로 만들면 Swift 계정 서버 접속 실패로 503 발생함

```bash
SWIFT_IP=$(ip -o -4 addr show dev eth0 | awk 'NR==1 {print $4}' | cut -d/ -f1)

sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/account.builder create 10 1 1
sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/account.builder add --region 1 --zone 1 --ip "$SWIFT_IP" --port 6002 --device sdb --weight 100
sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/account.builder rebalance

sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/container.builder create 10 1 1
sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/container.builder add --region 1 --zone 1 --ip "$SWIFT_IP" --port 6001 --device sdb --weight 100
sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/container.builder rebalance

sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/object.builder create 10 1 1
sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/object.builder add --region 1 --zone 1 --ip "$SWIFT_IP" --port 6000 --device sdb --weight 100
sudo docker run --rm -v /etc/kolla/config/swift:/etc/kolla/config/swift "$SWIFT_IMAGE" swift-ring-builder /etc/kolla/config/swift/object.builder rebalance
```

- ring 변경 후에는 실제 컨테이너가 읽는 경로(`/etc/kolla/swift`)에 동기화해야 함
 	- 동기화 누락 시 이전 IP로 접근해서 503 계속 발생함

```bash
sudo cp -a /etc/kolla/config/swift/*.builder /etc/kolla/swift/
sudo cp -a /etc/kolla/config/swift/*.ring.gz /etc/kolla/swift/
sudo chown root:root /etc/kolla/swift/*.builder /etc/kolla/swift/*.ring.gz
sudo chmod 660 /etc/kolla/swift/*.builder /etc/kolla/swift/*.ring.gz

sudo docker restart swift_account_server swift_container_server swift_object_server swift_proxy_server
```

- 503이 계속 나면 account/container 서버 포트가 뒤바뀐 경우 있음
 	- account는 6002, container는 6001이어야 함

```bash
sudo sed -i "s/bind_port = 6001/bind_port = 6002/" /etc/kolla/swift-account-server/account-server.conf
sudo sed -i "s/bind_port = 6002/bind_port = 6001/" /etc/kolla/swift-container-server/container-server.conf

sudo docker restart swift_account_server swift_container_server swift_proxy_server
```

- kolla-ansible 18.8.0 기본값이 account 6001/container 6002로 뒤집혀 있음
 	- `/etc/kolla/globals.yml`에 포트 오버라이드 추가해서 재발 방지함

```bash
swift_account_server_port: 6002
swift_container_server_port: 6001
```

- ring/서비스 포트 불일치 주의해야 함
 	- 위 ring 생성 명령은 account 6002/container 6001 기준임
 	- kolla-ansible 18.8.0 기본값은 account 6001/container 6002임
 	- 둘 중 하나 기준으로 통일해야 503 안 나옴
  		- 권장: `globals.yml` 오버라이드 유지 + `kolla-ansible reconfigure -t swift` 실행함
  		- 대안: ring을 kolla 기본값(6001/6002) 기준으로 다시 생성함

### 6) 배포

```bash
kolla-ansible -i ~/all-in-one bootstrap-servers
kolla-ansible -i ~/all-in-one prechecks
kolla-ansible -i ~/all-in-one pull

# 컨테이너 생성/설정/기동 + DB/서비스 초기화까지 수행하는 실제 배포 단계
kolla-ansible -i ~/all-in-one deploy

# 배포 후 클라이언트용 설정 파일 생성 단계
# - /etc/kolla/admin-openrc.sh, /etc/openstack/clouds.yaml 등을 만들어줌
# CLI 사용을 위한 설정 파일을 만드는 건 post-deploy
kolla-ansible -i ~/all-in-one post-deploy
```

- 최소 구성은 Keystone/Swift/Horizon만 활성화하고 나머지 OpenStack 서비스는 비활성화함
- core 의존성(MariaDB, RabbitMQ, Memcached, HAProxy/Keepalived, Cron, Fluentd)은
	자동으로 포함됨

### 7) OpenStack RC 로드

```bash
source /etc/kolla/admin-openrc.sh
```

- 이 단계부터 `openstack` CLI 사용 가능함

### 7-1) OpenStack CLI 설치

```bash
sudo /usr/local/bin/pip install python-openstackclient python-swiftclient
```

- `sudo`로도 실행하려면 `/usr/local/bin`이 secure_path에 포함되어야 함

```bash
sudo sh -c 'echo "Defaults secure_path=/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin" > /etc/sudoers.d/99-kolla-path'
```

## PoC 검증

### Keystone 확인

```bash
openstack token issue
+------------+--------------------------------------------------------------+
| Field      | Value                                                        |
+------------+--------------------------------------------------------------+
| expires    | 2026-01-10T08:40:56+0000                                     |
| id         | gAAAAABpYL8Y55NtsxNN8Mwo9GlcZ_khDBUdN9Iuzy6ET3PjspmlEtiQzapB |
|            | WmZT0peqCrbrjEORNRJeQmPfnKUf6w58b3SWzwdnKneaTT7rHKNwR2PqFHW6 |
|            | vQCD-R3c6PdD1PivjBIXUudnVFoHZ8UAGkl0J2-NF0D5FVagU02stIeJa06K |
|            | 5oE                                                          |
| project_id | 0152e680f129408b9acd3567c6b38222                             |
| user_id    | 0f542cb6b839411d84a1aa5e596e26bb                             |
+------------+--------------------------------------------------------------+
```

```bash
openstack endpoint list
```

- 토큰 발급 성공 및 서비스 엔드포인트 확인됨

### Keystone EC2 자격증명 생성

```bash
openstack ec2 credentials create
+------------+--------------------------------------------------------------+
| Field      | Value                                                        |
+------------+--------------------------------------------------------------+
| access     | 2a4c3810a3f14157b9dbbaa311a46dc2                             |
| links      | {'self': 'http://192.168.139.250:5000/v3/users/0f542cb6b8394 |
|            | 11d84a1aa5e596e26bb/credentials/OS-                          |
|            | EC2/2a4c3810a3f14157b9dbbaa311a46dc2'}                       |
| project_id | 0152e680f129408b9acd3567c6b38222                             |
| secret     | fdc71247d5ec49759cd20bdde581be35                             |
| trust_id   | None                                                         |
| user_id    | 0f542cb6b839411d84a1aa5e596e26bb                             |
+------------+--------------------------------------------------------------+
```

- `access`/`secret` 값이 S3 호환 API 인증에 사용됨
- 사용자/프로젝트별로 EC2 자격증명이 분리됨

### Swift 확인

```bash
openstack container create poc-container
openstack object create poc-container ./README.md
openstack object list poc-container
openstack object save poc-container README.md --file ./README.down
```

- 컨테이너 생성/업로드/다운로드까지 정상 동작 확인함

### S3 호환 API 확인

```bash
export AWS_ACCESS_KEY_ID="<openstack ec2 credentials create 결과 access>"
export AWS_SECRET_ACCESS_KEY="<openstack ec2 credentials create 결과 secret>"
export AWS_EC2_METADATA_DISABLED=true

aws --endpoint-url http://<kolla_internal_vip_address>:8080 s3 ls
aws --endpoint-url http://<kolla_internal_vip_address>:8080 s3 mb s3://poc-bucket
aws --endpoint-url http://<kolla_internal_vip_address>:8080 s3 ls
```

- Swift S3 API는 swift-proxy(8080)로 접근함
- `aws` CLI 미설치 시 `dnf -y install awscli`로 설치함

### Horizon GUI 확인

```bash
source /etc/kolla/admin-openrc.sh
echo "$OS_USERNAME / $OS_PASSWORD"
```

- 브라우저에서 `http://<kolla_internal_vip_address>` 접속함
- 도메인 `Default`, 사용자 `admin`, 비밀번호 `$OS_PASSWORD`, 프로젝트 `admin` 입력함

## 트러블슈팅

- `kolla-ansible install-deps` 실패
  - `requirements.yml`의 브랜치가 사라져서 발생함
   	- `stable/2024.1`을 `unmaintained/2024.1`로 변경 후 재시도 필요함
- `kolla-ansible` 실행 시 PATH 오류
 	- `export PATH=/usr/local/bin:$PATH` 필요함
- `python3-libselinux` 누락
 	- `sudo dnf -y install python3-libselinux` 설치 필요함
- `kolla_internal_vip_address`가 pingable
 	- 사용하지 않는 IP로 변경 필요함
- Swift ring 파일 없음
 	- `/etc/kolla/config/swift`에 ring 파일 생성 필요함
- 2025.1 태그에서 Swift 이미지 미존재함
 	- `swift-object:2025.1-rocky-9` 태그가 없어서 pull 실패함
 	- 2024.1-rocky-9로 내려서 진행 필요함
- Docker 권한 문제
 	- `sudo docker ...` 사용하거나 `sudo usermod -aG docker $USER` 필요함

### Swift 503 (포트 뒤바뀜)

- kolla-ansible 18.8.0 기본값이 account 6001/container 6002로 뒤집혀 있음
 	- ring은 account 6002/container 6001 기준으로 생성함
 	- 둘 중 하나 기준으로 통일해야 503 안 나옴
 	- 권장: `/etc/kolla/globals.yml`에 포트 오버라이드 추가함

```yaml
swift_account_server_port: 6002
swift_container_server_port: 6001
```

```bash
sudo sed -i "s/bind_port = 6001/bind_port = 6002/" /etc/kolla/swift-account-server/account-server.conf
sudo sed -i "s/bind_port = 6002/bind_port = 6001/" /etc/kolla/swift-container-server/container-server.conf
sudo docker restart swift_account_server swift_container_server swift_proxy_server
```

## 설치 진행 모니터링

- Kolla-Ansible 로그를 실시간 확인함

```bash
sudo tail -f /var/log/kolla/ansible.log
```

- systemd 로그를 `journalctl`로 실시간 확인함

```bash
sudo journalctl -f
```

- Docker 컨테이너 상태 확인함

```bash
sudo docker ps
```

## 실패 흔적 정리

- 컨테이너 및 볼륨 정리함

```bash
kolla-ansible -i ~/all-in-one destroy --yes-i-really-really-mean-it
sudo rm -rf /etc/kolla /var/log/kolla
```
