# MySQL Cluster (Cloud-Native) 운영 가이드

**MySQL Operator(InnoDB Cluster 3노드) + MySQL Router(단일 Service, 포트 6446/6450**

## 실행 방법

### 1. 변수 준비

`STORAGE_CLASS`, `ROOT_USER_PWD` 등 필요한 값을 설정한다.

### 2. 스크립트 실행

```bash
# 0) 사전 점검

# 1) Operator 설치
bash scripts/01-install-operator-helm.sh

# 2) 네임스페이스/Secret
bash scripts/02-create-namespace-and-secrets.sh

# 3) CR 적용(innodbcluster)
bash scripts/03-apply-innodbcluster.sh

# 4) 앱용 Router Service 적용
bash scripts/04-apply-router-service.sh

# 5) PDB 적용
bash scripts/05-apply-pdb.sh

# 6) 검증
bash scripts/06-verify.sh

# 7) 스모크 테스트(6446/6450 접속)
bash scripts/07-smoke-test.sh
```
