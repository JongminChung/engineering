# 용어집(Glossary)

_**Table of Contents**_

<!-- TOC -->

- [용어집(Glossary)](#용어집glossary)
  - [컴퓨트](#컴퓨트)
  - [네트워크](#네트워크)
  - [블록 스토리지](#블록-스토리지)
  - [오브젝트 스토리지](#오브젝트-스토리지)
  - [공통](#공통)
  - [보안](#보안)
  - [컨테이너/오케스트레이션](#컨테이너오케스트레이션)
  - [데이터베이스](#데이터베이스)
  - [캐시](#캐시)
  - [메시징/이벤트](#메시징이벤트)
  - [관측/로깅](#관측로깅)
  - [API 관리](#api-관리)

<!-- TOC -->

클라우드 인프라에서 자주 쓰이는 용어를 표준 용어 중심으로 정리합니다.
동일 의미의 용어가 여러 개라면 de facto 표준을 채택합니다.

## 컴퓨트

| 표준 용어   | 동의어         | 리소스 타입              | 설명                               | CSP 명칭(AWS/GCP/Azure)                                           |
|---------|-------------|---------------------|----------------------------------|-----------------------------------------------------------------|
| VM      | 인스턴스, 가상 머신 | Instance            | 하이퍼바이저 위에서 동작하는 범용 가상 서버.        | EC2 Instance / Compute Engine VM / Virtual Machine              |
| 인스턴스 타입 | 머신 타입, SKU  | Instance Type       | vCPU, 메모리, 네트워크 성능을 묶은 사양 단위.    | Instance Type / Machine Type / VM Size                          |
| 오토스케일링  | 자동 확장       | Auto Scaling Group  | 부하에 따라 인스턴스 수를 자동으로 늘리거나 줄이는 기능. | Auto Scaling / Autoscaler(MIG) / Autoscale                      |
| 이미지     | AMI, 머신 이미지 | Image               | OS와 기본 소프트웨어가 포함된 실행 템플릿.        | AMI / VM Image / VM Image                                       |
| 베어메탈    | 전용 서버       | Bare Metal Instance | 가상화 없이 물리 서버를 직접 제공하는 컴퓨트 형태.    | EC2 Bare Metal / Bare Metal Solution / BareMetal Infrastructure |
| 서버리스 함수 | 함수형 컴퓨트     | Function            | 이벤트 기반으로 실행되는 관리형 함수 컴퓨트.        | Lambda / Cloud Functions / Azure Functions                      |

## 네트워크

| 표준 용어     | 동의어           | 리소스 타입               | 설명                              | CSP 명칭(AWS/GCP/Azure)                                                    |
|-----------|---------------|----------------------|---------------------------------|--------------------------------------------------------------------------|
| VPC       | VNet, 가상 네트워크 | VPC                  | 클라우드에서 격리된 가상 네트워크 영역.          | VPC / VPC / VNet                                                         |
| 서브넷       | Subnet        | Subnet               | VPC 내부의 IP 대역 분할 단위.            | Subnet / Subnet / Subnet                                                 |
| 라우팅 테이블   | Route table   | Route Table          | 서브넷 간/외부 통신 경로를 정의하는 규칙 집합.     | Route Table / Routes / Route Table                                       |
| 보안 그룹     | SG, NSG       | Security Group       | 인스턴스 단위의 가상 방화벽(인바운드/아웃바운드 규칙). | Security Group / Firewall Rules / Network Security Group                 |
| NACL      | 네트워크 ACL      | Network ACL          | 서브넷 단위의 네트워크 ACL(규칙 우선순위 적용).   | Network ACL / 없음(대체: Firewall Rules) / Network Security Group(서브넷 적용)    |
| 로드 밸런서    | LB            | Load Balancer        | 여러 인스턴스로 트래픽을 분산하는 계층 4/7 서비스.  | ELB(ALB/NLB) / Cloud Load Balancing / Load Balancer(Application Gateway) |
| 퍼블릭 IP    | 공인 IP         | Public IP            | 인터넷에 노출되는 IP 주소.                | Elastic IP / External IP / Public IP                                     |
| 프라이빗 IP   | 사설 IP         | Private IP           | 내부 통신에 쓰는 IP 주소.                | Private IP / Internal IP / Private IP                                    |
| NAT 게이트웨이 | NAT           | NAT Gateway          | 프라이빗 네트워크의 아웃바운드 인터넷 통신용 게이트웨이. | NAT Gateway / Cloud NAT / NAT Gateway                                    |
| VPN 게이트웨이 | 사이트 간 VPN     | VPN Gateway          | 온프레미스와 클라우드 간 암호화 터널 연결.        | VPN / Cloud VPN / VPN Gateway                                            |
| 전용 회선     | 전용선           | Dedicated Connection | 전용 네트워크 회선으로 클라우드에 연결.          | Direct Connect / Cloud Interconnect / ExpressRoute                       |
| VPC 피어링   | VNet 피어링      | VPC Peering          | 서로 다른 VPC/VNet 간의 사설망 연결.       | VPC Peering / VPC Peering / VNet Peering                                 |
| DNS       | 도메인 서비스       | DNS Zone             | 도메인 이름 해석을 제공하는 서비스.            | Route 53 / Cloud DNS / Azure DNS                                         |
| CDN       | 콘텐츠 전송 네트워크   | CDN Distribution     | 캐시를 통해 콘텐츠를 전송하는 엣지 네트워크.       | CloudFront / Cloud CDN / Azure Front Door                                |

## 블록 스토리지

| 표준 용어   | 동의어      | 리소스 타입           | 설명                              | CSP 명칭(AWS/GCP/Azure)                                  |
|---------|----------|------------------|---------------------------------|--------------------------------------------------------|
| 블록 스토리지 | 디스크 스토리지 | Volume Service   | 디스크처럼 마운트되는 스토리지(파일시스템을 직접 구성). | EBS / Persistent Disk / Managed Disks                  |
| 볼륨      | 디스크      | Volume           | 블록 스토리지의 논리 단위(인스턴스에 연결/분리 가능). | EBS Volume / PD Disk / Managed Disk                    |
| 스냅샷     | Snapshot | Snapshot         | 특정 시점의 볼륨 상태를 저장한 백업 이미지.       | EBS Snapshot / PD Snapshot / Disk Snapshot             |
| IOPS    | -        | Performance Tier | 입출력 성능 지표(초당 처리 가능한 I/O 횟수).    | Provisioned IOPS / Provisioned IOPS / Provisioned IOPS |
| 프로비저닝   | 사전 할당    | Volume Type      | 요구 성능/용량을 미리 확보해 제공하는 방식.       | io1/io2, gp3 / Hyperdisk / Ultra Disk                  |

## 오브젝트 스토리지

| 표준 용어     | 동의어           | 리소스 타입         | 설명                            | CSP 명칭(AWS/GCP/Azure)                       |
|-----------|---------------|----------------|-------------------------------|---------------------------------------------|
| S3        | 오브젝트 스토리지     | Object Storage | 객체 단위로 저장하는 스토리지(버킷/키 기반).    | S3 / Cloud Storage / Blob Storage           |
| 버킷        | Bucket        | Bucket         | 오브젝트가 저장되는 최상위 컨테이너.          | Bucket / Bucket / Container                 |
| 오브젝트      | 객체, Object    | Object         | 버킷 내 데이터 단위.                  | Object / Object / Blob                      |
| 키         | Object key    | Object Key     | 버킷 내 오브젝트 식별자.                | Object Key / Object Name / Blob Name        |
| 프리사인드 URL | Presigned URL | Signed URL     | 인증 없이 제한 시간 동안 접근 가능한 URL.    | Pre-signed URL / Signed URL / SAS           |
| 스토리지 클래스  | Storage class | Storage Class  | 접근 빈도/보관 기간에 따라 비용/성능이 다른 등급. | Storage Class / Storage Class / Access Tier |
| 파일 스토리지   | NFS, 공유 스토리지  | File System    | 공유 파일 시스템 형태의 스토리지.           | EFS / Filestore / Azure Files               |

## 공통

| 표준 용어    | 동의어                            | 리소스 타입          | 설명                               | CSP 명칭(AWS/GCP/Azure)                                              |
|----------|--------------------------------|-----------------|----------------------------------|--------------------------------------------------------------------|
| 리전       | Region                         | Region          | 물리적으로 분리된 데이터센터 묶음.              | Region / Region / Region                                           |
| 가용 영역    | AZ, Availability Zone          | Zone            | 리전 내 독립 전원/네트워크를 가진 장애 도메인.      | Availability Zone / Zone / Availability Zone                       |
| IAM      | Identity and Access Management | Identity        | 사용자/역할/정책 기반의 인증·인가 시스템.         | IAM / IAM / Entra ID + Azure RBAC                                  |
| 매니지드 서비스 | 관리형 서비스                        | Managed Service | 운영(패치/백업/확장)을 클라우드가 대신 수행하는 서비스. | RDS, ElastiCache / Cloud SQL, Memorystore / Azure SQL, Azure Cache |

## 보안

| 표준 용어   | 동의어     | 리소스 타입      | 설명                          | CSP 명칭(AWS/GCP/Azure)                                                   |
|---------|---------|-------------|-----------------------------|-------------------------------------------------------------------------|
| KMS     | 키 관리    | Key         | 암호화 키의 생성/관리/회전 서비스.        | KMS / Cloud KMS / Key Vault(Keys)                                       |
| 시크릿 관리  | 비밀 관리   | Secret      | 비밀 값(토큰/비밀번호)을 저장하는 서비스.    | Secrets Manager / Secret Manager / Key Vault(Secrets)                   |
| WAF     | 웹 방화벽   | Web ACL     | L7 트래픽을 필터링하는 웹 애플리케이션 방화벽. | WAF / Cloud Armor / WAF                                                 |
| DDoS 보호 | -       | Protection  | 대규모 트래픽 공격 방어 서비스.          | Shield / Cloud Armor / DDoS Protection                                  |
| 인증서 관리  | TLS 인증서 | Certificate | TLS 인증서 발급/관리 서비스.          | Certificate Manager(ACM) / Certificate Manager / Key Vault Certificates |

## 컨테이너/오케스트레이션

| 표준 용어      | 동의어       | 리소스 타입       | 설명                 | CSP 명칭(AWS/GCP/Azure)                    |
|------------|-----------|--------------|--------------------|------------------------------------------|
| Kubernetes | K8s       | Cluster      | 컨테이너 오케스트레이션 플랫폼.  | EKS / GKE / AKS                          |
| 노드 풀       | 노드 그룹     | Node Pool    | 클러스터의 노드 집합.       | Node Group / Node Pool / Node Pool       |
| 컨테이너 레지스트리 | 이미지 레지스트리 | Repository   | 컨테이너 이미지 저장소.      | ECR / Artifact Registry / ACR            |
| 서버리스 컨테이너  | -         | Service/Task | 서버 관리 없이 컨테이너를 실행. | ECS Fargate / Cloud Run / Container Apps |

## 데이터베이스

| 표준 용어     | 동의어       | 리소스 타입              | 설명               | CSP 명칭(AWS/GCP/Azure)                       |
|-----------|-----------|---------------------|------------------|---------------------------------------------|
| 관계형 DB    | RDBMS     | DB Instance/Cluster | 관계형 데이터베이스 서비스.  | RDS/Aurora / Cloud SQL / Azure SQL Database |
| NoSQL     | 문서/키-값 DB | Table/Collection    | 비관계형 데이터베이스 서비스. | DynamoDB / Firestore / Cosmos DB            |
| 데이터 웨어하우스 | DW        | Cluster/Dataset     | 분석용 대규모 데이터 저장소. | Redshift / BigQuery / Synapse               |

## 캐시

| 표준 용어   | 동의어             | 리소스 타입        | 설명             | CSP 명칭(AWS/GCP/Azure)                             |
|---------|-----------------|---------------|----------------|---------------------------------------------------|
| 인메모리 캐시 | Redis/Memcached | Cache Cluster | 메모리 기반 캐시 서비스. | ElastiCache / Memorystore / Azure Cache for Redis |

## 메시징/이벤트

| 표준 용어        | 동의어     | 리소스 타입             | 설명                | CSP 명칭(AWS/GCP/Azure)               |
|--------------|---------|--------------------|-------------------|-------------------------------------|
| 메시지 큐        | MQ      | Queue              | 비동기 메시지 큐 서비스.    | SQS / Pub/Sub / Service Bus Queue   |
| 퍼블리시/서브스크라이브 | Pub/Sub | Topic/Subscription | 토픽 기반 메시지 전달.     | SNS / Pub/Sub / Service Bus Topic   |
| 이벤트 버스       | 이벤트 라우팅 | Event Bus          | 이벤트 라우팅/규칙 기반 전송. | EventBridge / Eventarc / Event Grid |
| 스트리밍         | 실시간 스트림 | Stream             | 대용량 스트리밍 데이터 처리.  | Kinesis / Pub/Sub / Event Hubs      |

## 관측/로깅

| 표준 용어 | 동의어   | 리소스 타입       | 설명               | CSP 명칭(AWS/GCP/Azure)                                         |
|-------|-------|--------------|------------------|---------------------------------------------------------------|
| 로그    | 로그 수집 | Log Group    | 로그를 수집/검색하는 서비스. | CloudWatch Logs / Cloud Logging / Azure Monitor Logs          |
| 메트릭   | 지표    | Metrics      | 성능 지표 수집/알림.     | CloudWatch Metrics / Cloud Monitoring / Azure Monitor Metrics |
| 트레이싱  | 분산 추적 | Trace        | 요청 흐름 추적 서비스.    | X-Ray / Cloud Trace / Application Insights                    |
| 알림    | 경보    | Alarm/Policy | 지표 기반 경보/알림.     | CloudWatch Alarm / Alerting Policy / Alert Rule               |

## API 관리

| 표준 용어     | 동의어    | 리소스 타입    | 설명                       | CSP 명칭(AWS/GCP/Azure)                      |
|-----------|--------|-----------|--------------------------|--------------------------------------------|
| API 게이트웨이 | API 관리 | API/Stage | API 호출을 제어하고 노출하는 게이트웨이. | API Gateway / API Gateway / API Management |
