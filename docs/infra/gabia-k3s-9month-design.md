# 가비아 gCloud 300만 원 크레딧 · 9개월 집중 운영 Kubernetes 최종 설계안

> 대상: Aligner 서버(Kotlin 2.4.10 / JDK 25 / Spring Boot 4.1.0 / PostgreSQL / Spring Data JDBC)
> 전제: 총 크레딧 3,000,000원, 운영 9개월, 월 예산 약 330,000원(VAT 포함)
> 원본 12개월(월 25만 원) 설계안의 재편성 버전

## 요금 단가 근거

이 문서의 모든 금액은 **가비아 클라우드 공식 요금 계산기(`www.gabiacloud.com/service/estimate`)가
사용하는 실제 단가표**에서 추출한 값이다. 추측값이 아니다. 확인된 VPC(Gen2) 단가는 다음이다.


| 항목                                       | 단가 (VAT 별도)                            | 비고                 |
| ---------------------------------------- | -------------------------------------- | ------------------ |
| micro `1vCPU/2GB` + Root SSD 50GB        | 25,750원/월                              |                    |
| high_cpu `2vCPU/4GB` + Root SSD 50GB     | 60,750원/월                              | 원본 설계안의 노드         |
| high_cpu `4vCPU/8GB` + Root SSD 50GB     | 115,750원/월                             |                    |
| **standard `2vCPU/8GB` + Root SSD 50GB** | **74,250원/월**                          | **본 설계안의 노드**      |
| standard `4vCPU/16GB` + Root SSD 50GB    | 142,750원/월                             |                    |
| high_memory `2vCPU/16GB` + Root SSD 50GB | 95,750원/월                              |                    |
| Root SSD 50GB → 100GB 변경                 | +5,750원/월                              | VM 요금에 Root SSD 포함 |
| 블록 스토리지 SSD(VPC)                         | 1,150원 / 10GB / 월 = **115원/GB**        | 10GB 단위, 최소 10GB   |
| External Load Balancer (Small)           | 15,000원/월                              |                    |
| 공인 IP                                    | 4,000원 / 개 / 월                         |                    |
| 무료 국내 트래픽                                | 서버·LB 등 **공인 IP 연동 장비당 1,110GB**(≈1TB) | 초과분 100원/GB        |
| 무료 해외 트래픽                                | 장비당 50GB                               | 초과분 500원/GB        |
| 스토리지 스냅샷 / 이미지                           | 건당 2,000원 / 1,000원 (1회성)               | 상시 백업 수단으로 쓰면 누적   |


> 원본 설계안의 “2vCPU/4GB 55,000원 + Root SSD 50GB 5,750원 = 60,750원”은 계산기 단가와
> 정확히 일치한다. 즉 원본의 견적 방식 자체는 검증됐고, 사양만 재배치하면 된다.
> 단 계산기 단가는 변경될 수 있으므로 **생성 직전 계산기에서 최종 확인**한다.

---

# 세션 1. 9개월 예산 최적화 아키텍처 &amp; 노드 스펙

## 1.1 예산 상한 산정


| 구분                     | 금액           |
| ---------------------- | ------------ |
| 총 크레딧                  | 3,000,000원   |
| 운영 기간                  | 9개월          |
| 월 사용 가능액 (VAT 포함)      | 333,333원     |
| **월 사용 가능액 (공급가액 환산)** | **303,030원** |


크레딧이 VAT까지 차감된다는 보수적 가정을 유지한다(§1.6 확인 항목). 크레딧을 100% 태우는
설계는 트래픽 초과·스냅샷 같은 변동비 한 번에 초과 결제로 넘어가므로, **소진 목표를 97~98%로
두고 잔액을 Phase 4 이관 리허설 재원으로 계획**한다.

## 1.2 후보안 비교 — 왜 `2vCPU/8GB × 3`인가

JVM 워크로드에서 부족하면 가장 먼저 서비스를 죽이는 자원은 **메모리**다. CPU 부족은 지연
증가로 나타나지만 메모리 부족은 OOMKill로 즉시 장애가 된다. 따라서 “같은 예산이면 메모리
비율이 높은 flavor”가 1차 기준이고, etcd quorum을 위한 **3노드**가 제약이다.


| 안          | 노드 구성                                       | 총 자원              | 월액(VAT 포함)   | 9개월 총액     | 판정              |
| ---------- | ------------------------------------------- | ----------------- | ------------ | ---------- | --------------- |
| **A (채택)** | standard `2/8` × 3                          | 6 vCPU / **24GB** | **301,895원** | 2,717,055원 | ✅ 예산 내, 잔액 9.4% |
| B          | high_cpu `4/8` × 3                          | 12 vCPU / 24GB    | 438,845원     | 3,949,605원 | ❌ 32% 초과        |
| C          | high_memory `2/16` × 3                      | 6 vCPU / 48GB     | 372,845원     | 3,355,605원 | ❌ 12% 초과        |
| D          | high_memory `2/16` × 2 + high_cpu `2/4` × 1 | 6 vCPU / 36GB     | 325,545원     | 2,929,905원 | ⚠️ 조건부          |
| E (원본)     | high_cpu `2/4` × 3                          | 6 vCPU / 12GB     | 249,755원     | 2,247,795원 | ❌ 크레딧 25% 미소진   |


**B 탈락** — CPU 12 vCPU는 매력적이지만 예산이 32% 초과한다. 6노드로 쪼개도 etcd 3 + worker 3
분리는 같은 금액대에서 노드당 사양이 다시 4GB로 내려가 원본 문제로 회귀한다.

**C 탈락** — 48GB는 이 규모에서 쓸 곳이 없다. 공인 IP·스토리지를 최소로 깎아도(320,600원 공급가)
9개월이 성립하지 않는다. 8개월이면 들어가지만 기간 요구를 못 맞춘다.

**D 조건부 대안** — 총 메모리 36GB로 A보다 12GB 많다. `2/16` 두 대에 앱을 올리고 `2/4` 한 대는
Control Plane·etcd·시스템 애드온 전용으로 taint를 건다. 단점이 결정적이다.

- 앱을 배치할 수 있는 노드가 **실질 2개**다. 한 대가 죽으면 전체 워크로드가 단일 노드로 몰린다.
- `topologySpreadConstraints`로 배울 수 있는 분산 시나리오가 2노드로 축소된다.
- 노드 사양이 불균일해 “어느 노드가 죽어도 같다”는 HA의 기본 성질이 깨진다. 장애 훈련 시
`2/4` 노드 정지와 `2/16` 노드 정지의 영향이 전혀 다르다.
- 공인 IP를 2개로 줄여야 겨우 예산에 들어가므로 예비비가 2.3%밖에 남지 않는다.

**A 채택** — 대칭 3노드는 스케줄링 예측 가능성, 장애 대응 절차의 단일성, 예비비 확보에서
전부 유리하다. 메모리 24GB는 Spring Boot API(heap 1GB급) 여러 개 + PostgreSQL HA 2대 +
Redis + 시스템 애드온을 **1노드 장애 여유까지 포함해** 수용한다(§3.5 검증).

원본(E) 대비 실질 변화는 다음 세 가지다.

1. 노드당 메모리 4GB → 8GB, 총 12GB → **24GB**
2. 원본이 포기한 **PostgreSQL HA(CloudNativePG 2 instance + PITR)** 를 되살림
3. 크레딧 소진율 75% → **98%** (원본을 9개월로 그냥 줄이면 75만 원이 남아 낭비)

## 1.3 채택안 월간 견적


| 항목                                          | 구성                         | 공급가액         |
| ------------------------------------------- | -------------------------- | ------------ |
| VM (standard `2vCPU/8GB`, Root SSD 50GB 포함) | × 3대                       | 222,750원     |
| 블록 스토리지 SSD (Data)                          | 60GB × 3대 = 180GB @115원/GB | 20,700원      |
| External Load Balancer (Small)              | 1개                         | 15,000원      |
| 공인 IP                                       | 노드 3 + LB 1 = 4개           | 16,000원      |
| **공급가액 합계**                                 |                            | **274,450원** |
| VAT 10%                                     |                            | 27,445원      |
| **월 합계**                                    |                            | **301,895원** |



| 기간 정산     | 금액              |
| --------- | --------------- |
| 9개월 기본 운영 | 2,717,055원      |
| 잔여 크레딧    | 282,945원 (9.4%) |


**잔여 크레딧 282,945원 집행 계획**


| 용도                                                        | 금액                       | 시점          |
| --------------------------------------------------------- | ------------------------ | ----------- |
| Phase 4 클러스터 이관 리허설용 임시 4번째 노드 (`2/8` + 공인 IP, 86,075원/월) | 172,150원                 | 8~9개월차, 2개월 |
| 트래픽 초과·스냅샷·이미지 변동비 예비                                     | 50,000원                  | 상시          |
| 최종 버퍼 (요금 인상·오차 흡수)                                       | 60,795원                  | —           |
| **예상 총 소진**                                               | **약 2,939,000원 (98.0%)** |             |


임시 4번째 노드는 낭비가 아니다. “새 클러스터를 처음부터 세워 데이터를 복구하고 트래픽을
넘긴다”는 DR 리허설은 **여분 노드 없이 검증이 불가능**하다. 잔액을 그 시점에 쓰도록 처음부터
설계에 넣는다.

## 1.4 Data SSD 180GB 배분

Root SSD 50GB에는 OS와 컨테이너 이미지만 둔다. **K3s 데이터 디렉터리와 etcd는 Data SSD로
분리**한다(`--data-dir=/mnt/data/k3s`). etcd는 fsync 지연에 민감하므로 이미지 pull·로그 I/O와
같은 디바이스를 쓰지 않는 것이 원칙이다.


| 노드당 60GB 용도                                   | 크기   |
| --------------------------------------------- | ---- |
| K3s data-dir + embedded etcd + 로컬 snapshot 보관 | 15GB |
| local-path PV (PostgreSQL primary 또는 standby) | 25GB |
| local-path PV (Redis, 기타)                     | 10GB |
| 여유 (증설 전 완충)                                  | 10GB |


블록 스토리지는 10GB 단위로 **증설**할 수 있으나 축소는 통상 불가하다. 따라서 60GB로 시작하고
사용률 70% 도달 시 10GB 단위로 늘린다(10GB당 1,265원/월 VAT 포함).

## 1.5 아키텍처 다이어그램

```text
                            Internet
                                │
                    ┌───────────┴────────────┐
                    │  Tailscale / VPN        │  관리 경로 (SSH·kubectl·Argo CD UI)
                    │  (무료, 노드에 데몬)     │  → 공인 IP에 22/6443 직노출 없음
                    └───────────┬────────────┘
                                │
        ┌───────────────────────┴───────────────────────────┐
        │        Gabia External Load Balancer (Small)        │
        │  :443  TCP/HTTPS  → Node:30443   (서비스 트래픽)   │
        │  :80   HTTP       → Node:30080   (ACME·리다이렉트) │
        │  :6443 TCP        → Node:6443    (K8s API, IP 제한)│
        │  공인 IP 1개 · 국내 1,110GB 무료                    │
        └───────────────────────┬───────────────────────────┘
                                │
     ┌──────────────────────────┼──────────────────────────┐
     │                          │                          │
┌────▼─────────────────┐ ┌──────▼───────────────┐ ┌────────▼─────────────┐
│ k3s-01               │ │ k3s-02               │ │ k3s-03               │
│ standard 2vCPU/8GB   │ │ standard 2vCPU/8GB   │ │ standard 2vCPU/8GB   │
│ Root SSD 50GB        │ │ Root SSD 50GB        │ │ Root SSD 50GB        │
│ Data SSD 60GB        │ │ Data SSD 60GB        │ │ Data SSD 60GB        │
│ 공인 IP 1개(아웃바운드)│ │ 공인 IP 1개          │ │ 공인 IP 1개          │
├──────────────────────┤ ├──────────────────────┤ ├──────────────────────┤
│  K3s server (role: 모두 동일 — 대칭 3노드)                              │
│  kube-apiserver / scheduler / controller-manager                       │
│  embedded etcd  (quorum 2/3, Data SSD)                                 │
│  kubelet / containerd / kube-proxy                                     │
│  Flannel VXLAN (8472/UDP)                                              │
├──────────────────────┤ ├──────────────────────┤ ├──────────────────────┤
│ Traefik              │ │ Traefik              │ │ Traefik              │
│ CoreDNS              │ │ CoreDNS              │ │ Argo CD (5 pods)     │
│ Grafana Alloy        │ │ Grafana Alloy        │ │ Grafana Alloy        │
│ cert-manager         │ │ CNPG standby         │ │ CNPG primary         │
│ Aligner API (Pod)    │ │ Aligner API (Pod)    │ │ Redis                │
│ local-path PV        │ │ local-path PV        │ │ local-path PV        │
└──────────┬───────────┘ └──────────┬───────────┘ └──────────┬───────────┘
           │                        │                        │
           └────────── Gabia Private Network (VPC) ──────────┘
              etcd 2379-2380/TCP · API 6443/TCP
              kubelet 10250/TCP · VXLAN 8472/UDP

           외부 백엔드 (클러스터 밖 — 클러스터가 죽어도 살아있음)
           ├─ Grafana Cloud   ← Alloy (metrics / logs / traces)
           ├─ AWS S3          ← etcd snapshot · PostgreSQL WAL·basebackup
           └─ GHCR            → 컨테이너 이미지 (digest 고정)
```

**트래픽 흐름 — 서비스**

```text
사용자 → LB:443 → Node:30443 (Traefik NodePort, externalTrafficPolicy: Local)
       → IngressRoute → Service → Aligner API Pod
```

`externalTrafficPolicy: Local`은 노드 간 홉을 제거하고 클라이언트 IP를 보존한다. 대신 Traefik이
없는 노드는 LB 헬스 체크에서 빠지므로 **Traefik을 3 replica로 노드마다 하나씩** 두어야 한다
(`topologySpreadConstraints` + `maxSkew: 1`).

**보안그룹 정책**


| 방향        | 포트                               | 허용 대상                            |
| --------- | -------------------------------- | -------------------------------- |
| 인바운드 (외부) | 443, 80                          | LB 사설 IP만 → NodePort 30443/30080 |
| 인바운드 (외부) | 6443                             | 관리자 고정 IP 또는 Tailscale 대역만       |
| 인바운드 (외부) | 22                               | Tailscale 대역만 (공인 IP 경유 SSH 차단)  |
| 인바운드 (사설) | 2379-2380, 6443, 10250, 8472/UDP | 노드 사설 대역만                        |
| 아웃바운드     | 443                              | 이미지 레지스트리·Grafana Cloud·S3·ACME  |


노드 공인 IP는 **아웃바운드 전용**으로 쓴다. SSH를 공인 IP로 열지 않고 Tailscale(무료 티어,
3 users / 100 devices) 오버레이를 관리망으로 쓰면 팀원 IP가 유동이어도 고정 IP 화이트리스트
운영이 필요 없고 22번 포트를 인터넷에 노출하지 않는다. 모든 K3s server에
`tls-san: k8s-api.aligner.example.com`을 넣어 LB 주소로 API에 접근한다.

## 1.6 구축 전 확인 필수 항목

원본 §10을 유지하되 이번 사양에서 새로 생긴 항목을 추가한다. **이 중 3·4번은 백업 설계를
바꿀 수 있으므로 Phase 1 착수 전에 답을 받아야 한다.**

1. 크레딧이 VAT를 포함해 차감되는가, 초과분이 자동 결제되는가, 자동 차단·알림 기능이 있는가
2. 월 한도가 실제 제한인가, 3,000,000원 통합 풀인가 (통합 풀이면 월별 편차 운용 가능)
3. **노드 간 사설망(VPC 내부) 트래픽이 무과금인가** — etcd 복제와 VXLAN은 상시 트래픽이다
4. **AWS S3(서울 리전)로의 업로드가 국내/해외 트래픽 중 어디로 분류되는가** — 해외로 분류되면
 무료 50GB를 넘는 순간 500원/GB다. etcd snapshot + PG basebackup은 월 수십 GB가 되므로
 해외 분류일 경우 백업 대상을 가비아 오브젝트 스토리지로 바꿔야 한다
5. NAT 게이트웨이 요금 — 노드 공인 IP 3개(12,000원)를 NAT로 대체하는 편이 싼지, 그리고 공인 IP를
 떼면 서버당 무료 트래픽 1,110GB가 어떻게 적용되는지
6. flavor 변경(`2/8` → `4/16`) 이 가능한지, 재부팅이 필요한지 — 스케일업 경로 확보용
7. 블록 스토리지 축소 가능 여부, 스냅샷 요금이 건당 1회성인지 보관 기간 과금인지

---

# 세션 2. 기술 스택 제로베이스 재검토

## 2.0 재검토 결과 요약


| 영역               | 원본 선택                 | **재검토 결론**                                    | 변경                      |
| ---------------- | --------------------- | --------------------------------------------- | ----------------------- |
| K8s 배포 도구        | K3s                   | **K3s** (2순위 RKE2)                            | 유지 · 근거 교체              |
| Control Plane DB | embedded etcd ×3      | **embedded etcd ×3** (Data SSD 분리)            | 유지 · 배치 개선              |
| GitOps           | Flux CD               | **Argo CD**                                   | **변경**                  |
| CNI              | Flannel VXLAN         | **Flannel VXLAN** + 표준 NetworkPolicy          | 유지 · 적용 시점 앞당김          |
| Service Proxy    | kube-proxy            | **kube-proxy**                                | 유지                      |
| Ingress          | Traefik (K3s 번들)      | **Traefik (번들 해제 후 Argo CD 관리)**              | 관리 주체 변경                |
| Secret           | SOPS + age            | **Sealed Secrets**                            | **변경** (Argo CD 채택의 귀결) |
| TLS              | cert-manager          | **cert-manager**                              | 유지                      |
| Storage          | local-path            | **local-path** (Longhorn 미도입)                 | 유지 · 근거 갱신              |
| Database         | PG 단일 + 백업            | **CloudNativePG 2 instance + Barman S3 PITR** | **변경**                  |
| Observability    | Alloy → Grafana Cloud | **Alloy → Grafana Cloud**                     | 유지 · 근거 강화              |
| 관리 접근            | 고정 IP 화이트리스트          | **Tailscale 오버레이**                            | 추가                      |
| 정책 엔진            | PSA + NetworkPolicy   | **PSA + NetworkPolicy**                       | 유지                      |


변경 3건은 모두 **월 예산이 25만 → 33만 원으로 늘어 메모리가 12GB → 24GB가 된 결과**다.
원본의 Flux·SOPS·PG 단일 Primary 선택은 “12GB 메모리”라는 제약의 산물이었고, 그 제약이
사라지면 결론이 달라진다. 예산이 늘었는데 스택이 그대로면 재검토를 하지 않은 것이다.

---

## 2.1 Kubernetes 배포 도구 — K3s vs kubeadm vs k0s  vs RKE2

### 비교


| 기준                  | K3s                                                                      | RKE2                                                                           | k0s                            | kubeadm                               |
| ------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------ | ------------------------------ | ------------------------------------- |
| 설치 형태               | 단일 바이너리, `curl | sh`                                                     | 단일 바이너리, upstream 컴포넌트를 static pod로                                            | 단일 바이너리, `k0sctl`로 선언적 구성      | upstream 표준 도구, 수동                    |
| Control Plane 실행 방식 | 단일 프로세스에 통합                                                              | **static pod (kubelet 관리)** — upstream과 동일                                     | 단일 프로세스                        | static pod                            |
| HA etcd             | embedded etcd 3서버                                                        | embedded etcd 3서버                                                              | embedded etcd 3컨트롤러            | 수동 구성 (stacked/external)              |
| 번들 컴포넌트             | CoreDNS, Traefik, ServiceLB, local-path, metrics-server, Helm controller | CoreDNS, Canal(Calico+Flannel), NGINX Ingress, metrics-server, Helm controller | CoreDNS, kube-router 또는 Calico | **없음** — 전부 직접                        |
| 메모리 오버헤드(서버 노드)     | 가장 낮음 (~0.5GB)                                                           | 중간 (~1.0GB, static pod 다중 프로세스)                                                | 낮음 (~0.6GB)                    | 중간 (~1.0GB)                           |
| 보안 하드닝              | 기본 수준                                                                    | **CIS Benchmark 프로파일 · FIPS 140-2 내장**                                         | 기본 수준                          | 직접 (kube-bench 등)                     |
| 업그레이드               | `system-upgrade-controller` 또는 바이너리 교체                                   | 동일                                                                             | `k0sctl apply`                 | `kubeadm upgrade` 수동 순차               |
| 인증서 갱신              | 재시작 시 자동 (만료 90일 이내)                                                     | 동일                                                                             | 자동                             | **수동** (`kubeadm certs renew`, 1년 만료) |
| 학습 전이성              | 높음 (kubectl·API 동일), 컴포넌트 내부 구조는 다름                                      | **가장 높음** (upstream 구조 그대로)                                                    | 높음                             | **최고** (CKA 시험 환경과 동일)                |
| 실무 채택 맥락            | 엣지·소규모·개발, 국내 스타트업 자체운영                                                  | 금융·공공·규제 환경 온프레미스                                                              | 상대적으로 사례 적음                    | 대기업 온프레미스, 시험                         |


### 판단: K3s 유지 — 단, 근거를 바꾼다

원본은 “번들 컴포넌트가 있어 편하다”를 주된 근거로 삼았다. 이건 약한 논거다. 편의성은
아키텍처 결정의 근거가 되기 어렵고, 실제로 이번 설계에서는 번들 Traefik·ServiceLB를
**둘 다 끄고** GitOps로 관리한다(§2.4). 진짜 근거는 셋이다.

**1) 9개월의 병목은 도구 학습이 아니라 운영 사이클 완주다.**
크레딧 만료가 확정된 9개월 안에 프로비저닝 → 배포 → 관측 → 백업 → 장애 훈련 → 이관까지
한 바퀴를 돌아야 한다. kubeadm HA는 여기서 순수 오버헤드를 만든다. Control Plane 앞단 LB
구성, stacked etcd 관리, **1년 만료 인증서 수동 갱신**, `kubeadm upgrade` 순차 절차를 직접
설계·문서화하는 데 최소 3~4주가 든다. 그 시간은 Aligner 서비스 운영 경험으로 치환되지 않는다.

**2) “실무 K8s 관리 경험”의 핵심은 배포 도구가 아니라 그 위의 운영이다.**
etcd 백업·복구, 노드 교체, 무중단 업그레이드, RBAC, NetworkPolicy, 스케줄링·자원 압박 대응,
PVC 노드 종속성 — 이 전부가 K3s에서 동일하게 발생하고 동일하게 배운다. kubectl과 API는
upstream과 같으므로 워크로드 레벨 경험은 100% 전이된다. K3s가 감춰주는 건 “컴포넌트를
프로세스로 어떻게 띄우는가”뿐이고, 그건 아래 3)으로 해결한다.

**3) Control Plane 오버헤드 절감분이 그대로 JVM heap이 된다.**
총 24GB에서 노드당 0.5GB(K3s) 대 1.0GB(RKE2/kubeadm)의 차이는 클러스터 전체로 1.5GB,
즉 **Spring Boot 파드 1개분**이다. 6 vCPU 환경에서 Control Plane CPU 절감도 무시할 수 없다.

**kubeadm 학습은 분리한다.** 운영 클러스터를 학습 실험장으로 쓰지 않는다는 원칙은 원본이
CNI에 적용한 것과 같다. `kubernetes-the-hard-way`(kelseyhightower)는 컴포넌트를 손으로
조립하며 CA·kubeconfig·etcd·apiserver 플래그를 이해하는 데 최적이고, **로컬 노트북의
멀티패스/VM 3대 또는 무료 티어에서 비용 0으로 수행 가능**하다. 크레딧을 여기에 태울 이유가 없다.
Phase 2 여유 시간에 별도 트랙으로 진행할 것을 권한다.

**RKE2 전환 조건** — 다음 중 하나가 생기면 RKE2가 더 낫다.

- CIS Benchmark 준수나 보안 감사 요구가 생김 (RKE2는 프로파일 하나로 적용)
- upstream 컴포넌트 구조를 그대로 다뤄야 하는 요구 (static pod, `/etc/kubernetes/manifests`)
- 노드 메모리가 16GB 이상으로 올라가 오버헤드 차이가 무의미해짐

**k0s 탈락** — 기술적으로 K3s와 대등하고 `k0sctl` 선언적 구성은 매력적이지만, 국내 운영 사례와
한국어/영어 트러블슈팅 자료가 K3s·RKE2에 비해 얇다. 9개월 단기전에서 “막혔을 때 검색해
나오는 양”은 실질적인 선정 기준이다.

### K3s 부트스트랩 설정

```yaml
# /etc/rancher/k3s/config.yaml (모든 server 노드)
data-dir: /mnt/data/k3s          # Data SSD로 분리 — etcd fsync를 Root SSD와 격리
tls-san:
  - k8s-api.aligner.example.com  # LB 주소로 API 접근
node-taint: []                    # 통합형: server도 워크로드 실행
disable:
  - traefik                       # Argo CD가 Helm으로 관리 (§2.4)
  - servicelb                     # 가비아 External LB와 역할 중복
etcd-snapshot-schedule-cron: "0 */6 * * *"   # 6시간마다
etcd-snapshot-retention: 28                  # 로컬 7일치
etcd-s3: true                                # S3 직접 업로드 (내장 기능)
kubelet-arg:
  - "system-reserved=cpu=200m,memory=512Mi"
  - "kube-reserved=cpu=200m,memory=512Mi"
  - "eviction-hard=memory.available<300Mi,nodefs.available<10%"
```

K3s의 **etcd snapshot S3 직접 업로드는 내장 기능**이다. 원본이 계획한 “로컬 스냅샷 후 주 1회
수동 S3 업로드” 대신 처음부터 S3로 보내고 로컬은 복구 속도용 캐시로만 둔다. 별도 CronJob이
필요 없다.

`system-reserved`/`kube-reserved`를 명시하는 이유는 8GB 노드에서 kubelet이 계산하는
allocatable을 예측 가능하게 만들기 위해서다. 이 값이 §3.5 자원 검증의 전제가 된다.

---

## 2.2 GitOps — Flux CD vs Argo CD → **Argo CD로 변경**

### 비교


| 기준          | Flux CD                                                            | Argo CD                                                       |
| ----------- | ------------------------------------------------------------------ | ------------------------------------------------------------- |
| 구성 요소       | source / kustomize / helm / notification controller                | api-server, repo-server, application-controller, redis, (dex) |
| 메모리 (최소 구성) | 약 150~250MB                                                        | 약 400~600MB                                                   |
| UI          | 없음 (`flux` CLI, 별도 Weave GitOps)                                   | **내장 Web UI** — 리소스 트리·live diff·sync 이벤트                     |
| 동기화 모델      | pull, 컨트롤러별 CR (`GitRepository` + `Kustomization` + `HelmRelease`) | pull, `Application` CR (app-of-apps 패턴)                       |
| SOPS 복호화    | **kustomize-controller에 내장**                                       | **내장 없음** — ksops·avp 등 플러그인 필요                               |
| Helm 처리     | helm-controller가 실제 Helm 릴리스로 설치                                   | 기본은 `helm template` 렌더링 (릴리스 아님)                              |
| 이미지 자동 갱신   | image-reflector/automation controller (선택)                         | Argo CD Image Updater (별도)                                    |
| 점진 배포       | Flagger                                                            | **Argo Rollouts** (카나리·블루그린)                                  |
| 공격면         | 노출 엔드포인트 없음                                                        | api-server 노출 시 인증·RBAC 관리 필요                                 |
| 팀 협업        | Git·CLI 숙련자 중심                                                     | 비운영 파트도 배포 상태 확인 가능                                           |


### 판단: Argo CD 채택

원본의 Flux 선택 근거는 “단일 클러스터·소수 운영자에서 더 단순하고 가볍다”였다. 이 문장은
사실이지만 결론을 뒤집는 요인이 세 개 있다.

**1) 프로젝트 정본 문서가 이미 Argo CD를 명기하고 있다.**
`README.md`의 기술 스택에 “인프라 — K3s 기반 HA Kubernetes (3 노드), **ArgoCD**”가 적혀 있다.
원본 인프라 보고서(Flux)와 프로젝트 정본(Argo CD)이 **불일치 상태**다. `AGENTS.md`는
“정본과 다르면 정본을 따른다”를 규칙으로 두고 있으므로, 인프라 보고서 쪽을 정본에 맞추는 것이
맞다. 반대로 Flux로 가려면 README를 먼저 고쳐야 한다.

**2) 원본이 Flux를 고른 물리적 이유(메모리)가 사라졌다.**
차이는 약 350MB다. 12GB 클러스터에서 350MB는 3%로 유의미했지만, 24GB에서는 1.5%다.
반면 UI가 주는 값은 규모와 무관하게 일정하다.

**3) 팀 구성이 UI를 요구한다.**
Server 2명(이강혁·이동훈), Web 3명, PM·Design 2명이다. 배포 상태를 확인해야 하는 사람이
kubectl 사용자보다 많다. “API가 배포됐는지”를 Web 파트가 직접 확인할 수 있으면
커뮤니케이션 비용이 줄고, 이건 5인 이상 팀에서 실측되는 이득이다. 또한 K8s 학습 국면에서
**live manifest와 Git 사이의 diff를 눈으로 보는 것**은 GitOps 개념 습득에 큰 차이를 만든다.

**Argo CD 채택의 대가는 정직하게 인정한다** — SOPS 내장 지원 상실이다. Flux
`kustomize-controller`는 SOPS를 내장하지만 Argo CD는 없다. 이것이 §2.6에서 Secret 전략을
바꾸는 직접 원인이다. 두 결정은 독립적이지 않다.

### 구성

```text
GitHub Actions (CI)
├─ ./gradlew build + ktlintCheck + integrationTest (TestContainers)
├─ 컨테이너 이미지 빌드 (Paketo buildpack, CDS 활성화 — §3.2)
├─ Trivy 취약점 스캔 (HIGH·CRITICAL에서 실패)
├─ GHCR push  →  ghcr.io/.../aligner-api@sha256:...
└─ GitOps 저장소의 kustomization.yaml 이미지 digest 갱신 (PR 또는 직접 커밋)
                            ↓
Argo CD (app-of-apps)
├─ platform/   : traefik, cert-manager, sealed-secrets, cnpg-operator, alloy  (sync-wave 0~1)
├─ database/   : CNPG Cluster, Redis                                          (sync-wave 2)
└─ aligner/    : API Deployment, Service, IngressRoute, HPA, PDB              (sync-wave 3)
                            ↓
Kubernetes (automated sync · prune · selfHeal)
```

- 자체 애플리케이션은 **Kustomize**, 외부 솔루션은 **Helm**(Argo CD가 렌더링)으로 관리한다.
`AGENTS.md`의 “Kustomize + Helm” 방침을 그대로 유지한다.
- 이미지는 **digest 고정**. `latest` 금지. Argo CD Image Updater는 초기에 쓰지 않는다 —
“Git이 유일한 진실”을 흐리고, 배포 시점 통제가 필요한 P0 단계에 적합하지 않다.
- `syncPolicy.automated.prune: true`, `selfHeal: true`. 수동 `kubectl apply`를 되돌려
드리프트를 원천 차단한다.
- **Argo CD UI 보안** — api-server를 인터넷에 그대로 노출하지 않는다. 기본은 Tailscale 경유
접근(`kubectl port-forward` 또는 Tailscale Ingress)이고, 팀 공유가 필요하면 Traefik
IngressRoute + **GitHub OAuth(Dex) + RBAC + `admin` 계정 비활성화**를 필수로 함께 적용한다.
인증 없는 Argo CD 노출은 클러스터 전체 권한 유출과 동등하다.

---

## 2.3 CNI — Flannel VXLAN vs Cilium (eBPF)


| 기준            | Flannel VXLAN                    | Cilium (eBPF)                                                                         |
| ------------- | -------------------------------- | ------------------------------------------------------------------------------------- |
| 데이터패스         | VXLAN 오버레이 + iptables            | eBPF, kube-proxy 대체 가능                                                                |
| 노드당 리소스       | ~50MB / 거의 0 CPU                 | agent ~~400~~600MB + operator, Hubble 추가 시 더                                          |
| NetworkPolicy | 표준 `NetworkPolicy` (K3s 내장 컨트롤러) | 표준 + `CiliumNetworkPolicy` (L7·DNS·FQDN)                                              |
| 관측성           | 없음 (tcpdump 수준)                  | **Hubble** — 흐름 단위 가시성                                                                |
| K3s 통합        | **기본 제공**                        | `flannel-backend=none` + `disable-network-policy` + (선택) `disable-kube-proxy` 후 수동 설치 |
| 장애 시 진단       | 이해할 표면이 좁음                       | eBPF·커널 버전·맵 상태까지 봐야 함                                                                |
| 교체 리스크        | —                                | CNI는 클러스터 생애 전체의 기반. 사후 교체 비용 최대                                                      |


### 판단: Flannel VXLAN 유지 — 단, NetworkPolicy는 Phase 1부터 적용

Cilium을 3노드 24GB에 넣으면 agent가 클러스터 메모리의 **6~~8%(1.5~~1.8GB)** 를 상시 점유한다.
Hubble Relay·UI까지 올리면 더 늘어난다. 그 대가로 얻는 eBPF 성능 이득은 서비스 수 10개 미만,
초당 수백 요청 규모에서 계측되지 않는다. Cilium의 실질 가치는 **규모와 멀티테넌시**에서
나오고, 우리는 둘 다 아니다.

원본과 다르게 하는 지점은 하나다. 원본은 NetworkPolicy를 “초기 필수 보안”으로만 언급했으나,
여기서는 **Phase 1의 완료 조건에 넣는다.** Flannel + K3s 내장 컨트롤러로 충분히 다음을 강제한다.

```text
default-deny-ingress (모든 애플리케이션 namespace)
├─ aligner-api      ← Traefik namespace 에서만 인바운드 허용
├─ postgres (CNPG)  ← aligner-api, cnpg-operator 에서만 5432 허용
├─ redis            ← aligner-api 에서만 6379 허용
└─ egress           → CoreDNS(53), 외부 443만 허용 (그 외 차단)
```

“CNI를 안 바꿨으니 네트워크 보안을 못 했다”가 되지 않게 하는 것이 핵심이다. L7 정책이
필요해지는 시점(서비스 간 인증, FQDN 기반 egress 통제)에 Cilium을 재검토한다.

**Cilium 검증 계획** — Phase 4에서 잔여 크레딧으로 띄우는 임시 노드/신규 클러스터에 Cilium을
`kube-proxy replacement` + Hubble로 설치해 검증한다. 이관 리허설과 CNI 평가를 같은 재원으로
동시에 수행하는 배치다. **운영 클러스터의 CNI를 학습 목적으로 교체하지 않는다**는 원본 원칙은
그대로 유지한다.

**노드 간 암호화** — 가비아 사설망이 테넌트 격리된다면 추가 암호화는 불필요하다(§1.6 확인 3번).
격리가 보장되지 않으면 K3s의 `flannel-backend=wireguard-native`로 노드 간 트래픽을 암호화한다.
설정 한 줄이고 CNI 교체가 아니다.

---

## 2.4 Ingress — Traefik vs NGINX Ingress Controller

**이 비교는 2026년 3월에 종료됐다.** `kubernetes/ingress-nginx`는 2026년 3월 은퇴했고 저장소는
read-only다. 이후 릴리스·버그 수정·**보안 취약점 패치가 전혀 없다**. 2026년 8월 시점에 신규
클러스터의 인그레스로 선택하는 것은 알려진 미패치 취약점을 받아들이는 결정이다. 후보에서 제외한다.

### 남은 후보 비교


| 후보                   | 평가                                                                                                           |
| -------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Traefik**          | K3s 생태계 표준, Ingress·Gateway API·CRD(IngressRoute) 모두 지원, 단일 Go 바이너리(~100MB), 미들웨어(rate limit·헤더·인증)를 CRD로 선언 |
| Envoy Gateway        | Gateway API 정통 구현, 기능·성능 우수하나 컨트롤 플레인 + Envoy 프록시 2계층으로 리소스·개념 부담                                            |
| Cilium Gateway       | Cilium을 이미 쓸 때만 타당. §2.3에서 Cilium을 안 쓰므로 탈락                                                                  |
| InGate               | ingress-nginx 후속 SIG-Network 프로젝트. 성숙도가 아직 프로덕션 기준 미달                                                        |
| NGINX Gateway Fabric | F5 주도, nginx 기반 Gateway API 구현. 대안이나 Traefik 대비 이점 없음                                                        |


### 판단: Traefik — 단, K3s 번들을 끄고 Argo CD가 관리한다

원본은 “K3s 기본 패키지에 포함되어 추가 설치가 필요 없다”를 근거로 삼았다. 이 부분은 바꾼다.
K3s 번들 Traefik은 `HelmChartConfig` CR로만 설정을 덮어쓸 수 있고 **버전이 K3s 버전에 묶인다**.
GitOps를 도입하는 이상 인그레스 버전과 설정이 Git에 없는 상태는 일관성 위반이다.

```text
K3s config.yaml:  disable: [traefik]
Argo CD platform/traefik:  Helm chart (버전 고정) + values.yaml (Git)
```

이렇게 하면 Traefik 업그레이드가 Git PR이 되고, 롤백이 `git revert`가 된다. K3s 업그레이드와
Traefik 업그레이드를 분리할 수 있는 것도 이득이다(Phase 4 업그레이드 리허설에서 중요).

**핵심 설정**

```yaml
deployment:
  replicas: 3                          # 노드당 1개 — externalTrafficPolicy: Local의 전제
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: kubernetes.io/hostname
    whenUnsatisfiable: DoNotSchedule
service:
  type: NodePort
  externalTrafficPolicy: Local         # 클라이언트 IP 보존 + 노드 간 홉 제거
ports:
  web:      { nodePort: 30080, redirectTo: { port: websecure } }
  websecure: { nodePort: 30443 }
podDisruptionBudget:
  enabled: true
  minAvailable: 2                      # 업그레이드 중에도 2노드가 LB 헬스체크 통과
```

`PodDisruptionBudget minAvailable: 2`가 중요하다. Traefik이 1개만 남으면 LB 뒤 정상 노드가
1개로 줄어 단일 장애점이 된다.

**TLS는 cert-manager** — Traefik 내장 ACME는 replica 간 인증서 저장소를 공유하지 못해
3 replica에서 중복 발급·rate limit 문제가 생긴다. cert-manager + Let's Encrypt로 인증서를
Secret에 중앙 관리하고 Traefik이 참조한다. 원본 판단과 동일하다.

---

## 2.5 Storage &amp; Database

### 2.5.1 local-path vs Longhorn


| 기준       | local-path Provisioner | Longhorn                                            |
| -------- | ---------------------- | --------------------------------------------------- |
| 노드 요구 사양 | 없음                     | **프로덕션 권장 노드당 4 vCPU / 4GiB (V1 Data Engine)**      |
| 리소스 점유   | 거의 0                   | manager + instance-manager, 복제 동기화 시 CPU·네트워크 상시 소모 |
| 복제       | 없음 — PV가 노드에 고정        | 블록 레벨 replica 2~3, 노드 장애 시 자동 재연결                   |
| 장애 시     | 해당 노드 복구까지 PVC 사용 불가   | 다른 노드에서 즉시 attach                                   |
| 백업       | 직접 구성                  | S3 백업·스냅샷 내장                                        |


**판단: local-path 유지.** 메모리는 8GB로 늘었지만 **CPU는 2 vCPU 그대로**이고, Longhorn의
프로덕션 권장 사양(4 vCPU)에 미달한다. instance-manager의 replica 동기화는 CPU 바운드 작업이라
2 vCPU 노드에서 JVM과 정면 경합한다. 스토리지 복제를 얻으려고 애플리케이션 성능을 내주는
교환이다.

**대신 복제를 애플리케이션 계층에서 해결한다** — 이게 이번 설계의 핵심 전환이다.

```text
Longhorn (블록 레벨 복제)          →  PostgreSQL 스트리밍 복제 (논리 계층)
  CPU 비용 큼, 범용                    CPU 비용 작음, DB에 특화
  2 vCPU 노드에서 JVM과 경합            PostgreSQL이 원래 하는 일
```

상태를 가진 워크로드는 사실상 PostgreSQL과 Redis뿐이다. PostgreSQL은 스트리밍 복제로,
Redis는 캐시로만 쓰고 유실을 허용하면(또는 AOF + 재구성) 범용 분산 스토리지가 필요 없다.

**Longhorn 실험은 Phase 4로.** 비핵심 namespace, replica 2, 테스트 데이터로만 검증하고 결과를
문서화한다. 운영 StorageClass로 승격하지 않는다.

### 2.5.2 PostgreSQL — 단일 Primary에서 CNPG HA로

원본은 “12GB 메모리에서 Control Plane·앱·관측성까지 확보해야 하므로 PG 3 replica를 하지
않는다”고 했다. 정확한 판단이었다. **24GB에서는 전제가 바뀐다.**

**채택: CloudNativePG Operator, `instances: 2`**

```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Cluster
metadata:
  name: aligner-db
spec:
  instances: 2                    # primary 1 + hot standby 1
  imageName: ghcr.io/cloudnative-pg/postgresql:17   # 버전 고정
  primaryUpdateStrategy: unsupervised               # switchover 후 롤링 업데이트

  storage:
    size: 20Gi
    storageClass: local-path      # 각 인스턴스가 자기 노드의 local PV 사용
  walStorage:
    size: 5Gi
    storageClass: local-path      # WAL을 데이터와 분리

  affinity:
    enablePodAntiAffinity: true
    topologyKey: kubernetes.io/hostname   # primary와 standby를 다른 노드로 강제

  postgresql:
    parameters:
      max_connections: "120"
      shared_buffers: "512MB"
      effective_cache_size: "1536MB"
      work_mem: "8MB"
      maintenance_work_mem: "128MB"
      wal_compression: "on"

  resources:
    requests: { cpu: 400m, memory: 2Gi }
    limits:   { cpu: 1500m, memory: 2Gi }   # 메모리 requests = limits

  # Barman Cloud Plugin → S3: WAL 아카이빙 + 주간 basebackup + PITR
  plugins:
    - name: barman-cloud.cloudnative-pg.io
      parameters:
        barmanObjectName: aligner-s3-backup
```

**설계 판단**

- `**instances: 2`이고 3이 아닌 이유** — 3 인스턴스는 이상적이지만 노드 3개 중 3개 모두에
PG가 올라가 앱 스케줄링 여유가 사라진다. 2 인스턴스(primary + standby)로 **자동 failover와
읽기 분산**을 얻고, 세 번째 사본은 S3의 WAL 아카이브가 담당한다.
- **비동기 복제를 쓴다.** standby 1대에 동기 복제(`synchronous_commit=on` + 
`synchronous_standby_names`)를 걸면 standby 장애 시 **primary의 쓰기가 멈춘다**. 
가용성이 오히려 내려간다. Aligner의 데이터 특성(세션 기록·스탬프)은 수 초의 RPO를 허용한다.
- **메모리 `requests = limits = 2Gi`** — PostgreSQL은 `shared_buffers`를 미리 잡고 반납하지
않으므로 Burstable로 두면 노드 압박 시 eviction 대상이 된다. JVM과 같은 이유다(§3.1).
- `**shared_buffers 512MB**` — 컨테이너 limit 2Gi의 25%. 일반 권장(RAM 25%)을 따르되
나머지를 OS 페이지 캐시와 work_mem에 남긴다.
- CNPG는 `-rw`, `-ro`, `-r` 서비스를 자동 생성한다. Spring Boot는 쓰기에 `aligner-db-rw`,
읽기 전용 Query 서비스에 `aligner-db-ro`를 쓸 수 있다.
`docs/architecture.md`의 Command/Query 분리와 자연스럽게 맞물린다.

**백업·복구 전략 (RPO / RTO 명시)**


| 대상                    | 방식                   | 주기      | 보관                  | RPO       | RTO 목표             |
| --------------------- | -------------------- | ------- | ------------------- | --------- | ------------------ |
| etcd                  | K3s 내장 snapshot → S3 | 6시간     | 로컬 28개(7일) / S3 30일 | 6h        | 30분                |
| PostgreSQL WAL        | Barman Cloud → S3    | 연속 아카이빙 | 30일                 | **5분 이내** | 1시간                |
| PostgreSQL basebackup | Barman Cloud → S3    | 주 1회    | 4주                  | —         | 1시간                |
| K8s Manifest          | Git (GitOps 저장소)     | 커밋마다    | 영구                  | 0         | 15분 (Argo CD sync) |
| Secret 원본             | 팀 패스워드 매니저           | 변경 시    | 영구                  | 0         | —                  |
| 컨테이너 이미지              | GHCR (digest 고정)     | 빌드마다    | —                   | 0         | —                  |


**복구 리허설을 하지 않은 백업은 백업이 아니다.** 월 1회 복구 훈련을 Phase 2부터 정례화하고,
결과(소요 시간·문제점)를 기록한다. 이게 Phase 4 DR 훈련의 기반이 된다.

---

## 2.6 Secret &amp; Security — SOPS + age vs External Secrets Operator vs Vault

Argo CD 채택(§2.2)이 이 결정을 바꾼다. Flux는 SOPS를 내장하지만 Argo CD는 없다.


| 기준           | SOPS + age (via ksops)            | **Sealed Secrets** | ESO + AWS Secrets Manager | Vault                             |
| ------------ | --------------------------------- | ------------------ | ------------------------- | --------------------------------- |
| Argo CD 조립   | repo-server에 플러그인 사이드카·커스텀 이미지 필요 | **CRD만 — 마찰 없음**   | CRD만 — 마찰 없음              | CRD(ESO 경유) 또는 Agent Injector     |
| 리소스          | 0 (렌더링 시점 처리)                     | 컨트롤러 1개, ~50MB     | 컨트롤러 ~150MB               | **Vault HA Raft 3노드 — 이 예산에서 과잉** |
| 외부 의존        | 없음                                | 없음                 | AWS (시크릿당 $0.40/월)        | 자체 운영                             |
| 비용           | 0                                 | **0**              | 시크릿 10개 ≈ 5,600원/월        | 0 (자원 비용은 큼)                      |
| Git diff 가독성 | **좋음** (변경된 키만 바뀜)                | 나쁨 (전체 재암호화)       | 최상 (Git에 참조만)             | 최상                                |
| 로컬에서 값 확인    | **가능** (age 키 보유자)                | 불가                 | AWS 콘솔에서 가능               | Vault UI                          |
| 자동 로테이션      | 없음                                | 없음                 | 있음                        | 있음                                |
| 키 분실 리스크     | age 키 분실 → 복호화 불가                 | 클러스터 키 분실 → 복호화 불가 | 없음 (AWS가 보관)              | Unseal 키 분실                       |


### 판단: Sealed Secrets 채택

**근거**

1. **Argo CD와 조립 마찰이 0이다.** `SealedSecret` CRD를 그대로 Git에 두면 컨트롤러가 클러스터
 안에서 `Secret`으로 변환한다. Argo CD repo-server에 커스텀 플러그인·사이드카를 넣지 않는다.
 ksops 경로는 repo-server 커스텀 이미지 유지보수 부담을 추가하고, 이는 “운영 요소를 늘리지
 않는다”는 원본 원칙에 정면으로 어긋난다.
2. **외부 의존과 비용이 0이다.** ESO + AWS Secrets Manager는 깔끔하지만 AWS 크레딧에 의존한다.
 크레딧이 9개월 안에 만료되거나 정책이 바뀌면 시크릿 계층 전체가 영향을 받는다. 백업 저장소로
 S3를 쓰는 것(유실되어도 재생성 가능)과 시크릿 원본을 외부에 두는 것은 리스크 등급이 다르다.
3. 컨트롤러 하나(50MB)로 끝난다.

**Sealed Secrets의 약점 두 개를 운영 규칙으로 덮는다**

- **값을 로컬에서 읽을 수 없다** → 시크릿의 **정본은 팀 패스워드 매니저(1Password/Bitwarden)**
로 정한다. `SealedSecret`은 배포 산출물이고 정본이 아니다. 이건 우회가 아니라 실무 정석이다.
Git에 있는 암호문을 진실의 원천으로 삼는 관행 자체가 위험하다.
- **클러스터 키 분실 시 전부 복호화 불가** → 부트스트랩 직후
`kubectl -n kube-system get secret -l sealedsecrets.net/sealed-secrets-key -o yaml`을
덤프해 **오프라인 암호화 보관**한다. 이 절차는 Phase 1 완료 조건이다. 새 클러스터로 이관할 때
이 키를 복원하면 Git의 모든 `SealedSecret`이 그대로 동작한다 — Phase 4 이관 리허설의
필수 전제다.

**전환 조건**


| 상황                                   | 전환 대상                       |
| ------------------------------------ | --------------------------- |
| 시크릿 20개 초과, 자동 로테이션 필수, 다중 클러스터 공유   | ESO + AWS Secrets Manager   |
| 애플리케이션별 동적 DB 자격증명, 내부 PKI, 감사 로그 요구 | Vault                       |
| GitOps를 Flux로 되돌림                    | SOPS + age (Flux 내장 경로가 최선) |


### 보안 기준선 (정책 엔진 없이)

원본의 “PSA + NetworkPolicy, Kyverno/OPA 미도입”은 유지한다. 다만 항목을 구체화한다.


| 항목                     | 설정                                                                                                          |
| ---------------------- | ----------------------------------------------------------------------------------------------------------- |
| Pod Security Admission | 애플리케이션 namespace에 `restricted` (enforce). 시스템 namespace는 예외 라벨                                              |
| 컨테이너                   | `runAsNonRoot: true`, `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`, 모든 capability drop |
| NetworkPolicy          | default-deny ingress/egress + 명시적 허용만 (§2.3)                                                                |
| ServiceAccount         | `automountServiceAccountToken: false` (필요한 것만 예외)                                                           |
| 이미지                    | digest 고정, Trivy 스캔 게이트, GHCR private                                                                       |
| RBAC                   | Argo CD `Application`마다 최소 권한, cluster-admin 상시 사용 금지                                                       |
| 감사                     | K3s audit log 활성화(`kube-apiserver-arg: audit-log-path`), Alloy가 Grafana Cloud로 전송                           |


Kyverno·Gatekeeper를 넣지 않는 이유는 리소스가 아니라 **운영 주체가 2명**이라는 점이다.
정책 엔진은 정책을 작성·검토·예외 관리할 인력이 있을 때 가치가 있다. 위 기준선을
`kustomize` base에 넣어 모든 앱이 상속하게 하면 같은 효과의 90%를 인력 비용 없이 얻는다.

---

## 2.7 Observability — Alloy(외부) vs 클러스터 내 Prometheus/Loki

메모리가 24GB로 늘었으니 내부 스택을 재검토할 조건이 생겼다. 실제로 계산해 본다.


| 구성 요소 (클러스터 내 스택)                         | 메모리                            | 디스크          |
| ----------------------------------------- | ------------------------------ | ------------ |
| Prometheus (retention 15일, 3노드 + 25 pods) | 1.5~3GB                        | 20~40GB      |
| Grafana                                   | 200~300MB                      | 1GB          |
| Alertmanager (HA 2 replica)               | 200MB                          | —            |
| Loki (single binary + filesystem)         | 500MB~1GB                      | 30~60GB      |
| kube-state-metrics + node-exporter ×3     | 250MB                          | —            |
| **합계**                                    | **2.7~~4.8GB (24GB의 11~~20%)** | **50~100GB** |



| 구성 요소 (Alloy 외부 전송)        | 메모리                | 디스크        |
| -------------------------- | ------------------ | ---------- |
| Grafana Alloy DaemonSet ×3 | 3 × 200MB = 600MB  | 0 (WAL 소량) |
| kube-state-metrics         | 150MB              | —          |
| metrics-server (HPA용, 필수)  | 100MB              | —          |
| **합계**                     | **약 850MB (3.5%)** | **거의 0**   |


### 판단: Alloy → Grafana Cloud 유지. 근거를 강화한다

**1) 관측성 백엔드를 관측 대상과 같은 클러스터에 두면 장애 시 진단 수단을 동시에 잃는다.**
이게 결정적 논거다. 9개월 계획의 후반 3개월이 장애 훈련과 DR이다. 노드를 강제로 죽이고
etcd를 복구하는 훈련에서 **Prometheus가 그 클러스터 안에 있으면 훈련 중 대시보드가 함께
사라진다.** 무엇이 언제 어떻게 죽었는지 기록이 남지 않으면 훈련의 절반이 무의미해진다.
게다가 Prometheus PVC를 local-path에 두면 그 노드가 죽는 순간 메트릭 이력까지 잃는다.

**2) 절감된 3~~4GB와 50~~100GB 디스크가 곧 JVM heap과 PG standby다.**
디스크 100GB는 Data SSD 기준 11,500원/월(공급가)이다. 9개월이면 약 10만 원 — 잔여 크레딧의
36%를 관측성 저장소가 먹는다.

**3) 클러스터 전체 다운을 외부에서 감지할 수 있다.**
Grafana Cloud의 alerting과 외부 프로빙(Synthetic Monitoring)을 쓰면 클러스터가 완전히 죽어도
Slack·Discord로 알림이 온다. 내부 Alertmanager는 클러스터와 함께 침묵한다.

### 구성

```text
Spring Boot Pod
├─ Actuator /actuator/prometheus  (Micrometer)  ─┐
├─ OTel Java Agent → OTLP :4317              ─┤
└─ stdout (JSON 구조화 로그)                  ─┤
                                              │
Kubernetes 노드 (DaemonSet)                    │
  Grafana Alloy ◄────────────────────────────┘
  ├─ prometheus.scrape        (pod annotation 기반 자동 발견)
  ├─ loki.source.kubernetes   (컨테이너 로그)
  ├─ otelcol.receiver.otlp    (트레이스)
  ├─ prometheus.relabel       ★ 카디널리티 제어 (아래)
  └─ *.write / *.exporter → Grafana Cloud (Mimir / Loki / Tempo)

클러스터 내부에 두는 것:  metrics-server(HPA 필수), kube-state-metrics
클러스터 내부에 두지 않는 것:  Prometheus TSDB, Loki, Grafana, Alertmanager, Elasticsearch
```

**Free tier 한도 관리 — Spring Boot 특화 주의점**

Grafana Cloud 무료 티어는 활성 시리즈 수에 제한이 있다. Spring Boot의 기본 메트릭이 이 한도를
가장 빨리 태우는 원인이므로 **Alloy에서 relabel로 잘라낸다.**


| 문제                                                                      | 대응                                                                                                                           |
| ----------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `http_server_requests_seconds`의 `uri` 라벨이 실제 경로별로 폭발 (`/courses/12345`) | Spring MVC는 기본적으로 `@PathVariable` 템플릿(`/courses/{id}`)을 사용한다. 커스텀 `MeterFilter`로 미매칭 요청(`uri="/**"`)을 묶고, `exception` 라벨은 제거 |
| `jvm_buffer_*`, `tomcat_*`, `logback_*` 등 저활용 메트릭 다수                    | Alloy `prometheus.relabel`에서 `drop`                                                                                          |
| histogram 버킷 수 (`percentiles-histogram`)                                | SLO 측정에 필요한 엔드포인트만 활성화                                                                                                       |
| 파드 재생성마다 `pod` 라벨이 새 시리즈 생성                                             | `pod` 라벨을 유지할 메트릭을 선별 (JVM 메트릭은 유지, HTTP 메트릭은 `deployment` 단위 집계)                                                            |


**반드시 유지해야 할 메트릭** — 이 설계의 자원 튜닝 근거가 되는 것들이다.

```text
jvm_memory_used_bytes{area="heap"}        → limits.memory 적정성 판단
jvm_memory_committed_bytes               → MaxRAMPercentage 검증
jvm_gc_pause_seconds                     → heap 부족 징후 (GC 빈도·시간 증가)
jvm_threads_live_threads                 → 스레드 스택 메모리 추정
process_cpu_usage / system_cpu_usage     → requests.cpu 실측
container_cpu_cfs_throttled_seconds_total → ★ CPU limit 스로틀링 탐지 (§3.1)
http_server_requests_seconds{quantile}   → p95·p99 지연
hikaricp_connections_pending             → 커넥션 풀 포화
```

`container_cpu_cfs_throttled_seconds_total`은 JVM 워크로드에서 가장 중요한 메트릭인데 자주
누락된다. 이 값이 올라가면 CPU limit이 낮아 JIT·GC 스레드가 강제로 멈추고 있다는 뜻이다.

---

# 세션 3. Kotlin + Spring Boot 운영 최적화 가이드

전제: Kotlin 2.4.10 / JDK 25 (Amazon Corretto) / Spring Boot 4.1.0 / Spring Data JDBC.
노드는 `2 vCPU / 8GB` × 3이다. 이 사양이 아래 모든 숫자의 근거다.

## 3.1 자원 할당 원칙 — JVM은 일반 워크로드와 다르게 다룬다

### 원칙 1. 메모리는 `requests == limits` (Guaranteed)

```yaml
resources:
  requests: { memory: 1536Mi }
  limits:   { memory: 1536Mi }   # 동일하게
```

**이유** — JVM은 한 번 확보한 메모리를 OS에 거의 반납하지 않는다. heap이 커진 뒤 부하가
빠져도 RSS는 그대로 유지된다. 이 상태에서 `requests < limits`(Burstable)로 두면 노드 메모리
압박 시 kubelet의 eviction 우선순위에서 **Burstable 파드가 먼저 축출**된다. 즉 “가끔 많이
쓰는 게 아니라 계속 많이 쓰는” JVM에 Burstable은 잘못된 신호다.

또한 Kubernetes 메모리 limit 초과는 **스로틀링이 아니라 OOMKill**이다. CPU와 달리 완충이 없다.
컨테이너가 limit에 닿으면 커널이 즉시 프로세스를 죽인다.

### 원칙 2. 컨테이너 메모리는 heap이 아니다

```text
컨테이너 RSS = Java heap
             + Metaspace (클래스 메타데이터, Spring은 큼 — 100~200MB)
             + Code Cache (JIT 컴파일 결과 — 50~150MB)
             + Thread stacks (스레드 수 × 1MB — Tomcat 200스레드면 200MB)
             + Direct/Mapped ByteBuffer (NIO, JDBC 드라이버)
             + GC 구조체 (G1은 heap의 약 5~10%)
             + JVM 자체 native
```

`-Xmx`만 잡고 컨테이너 limit을 같은 값으로 주면 **반드시 OOMKill된다.** 실무 배분은 다음이다.


| 컨테이너 limit | `MaxRAMPercentage` | 예상 heap     | non-heap 여유     |
| ---------- | ------------------ | ----------- | --------------- |
| 1024Mi     | 70                 | ~717MB      | ~307MB (빡빡함)    |
| **1536Mi** | **70**             | **~1075MB** | **~461MB (권장)** |
| 2048Mi     | 75                 | ~1536MB     | ~512MB          |


**Aligner API 권장값**

```yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: >-
      -XX:MaxRAMPercentage=70.0
      -XX:InitialRAMPercentage=50.0
      -XX:+UseG1GC
      -XX:MaxMetaspaceSize=256m
      -XX:+ExitOnOutOfMemoryError
      -XX:+HeapDumpOnOutOfMemoryError
      -XX:HeapDumpPath=/tmp/heapdump.hprof
      -Xss512k
resources:
  requests: { cpu: 400m,  memory: 1536Mi }
  limits:   { cpu: 2000m, memory: 1536Mi }
```

- `-Xmx` 고정값 대신 `**MaxRAMPercentage**` 를 쓴다. 컨테이너 limit을 바꿀 때 JVM 옵션을
같이 고쳐야 하는 이중 관리가 사라진다. JDK 10+ `UseContainerSupport`가 기본 활성이므로
cgroup limit을 정확히 읽는다.
- `InitialRAMPercentage=50` — heap을 처음부터 절반 확보해 시작 직후 heap 확장에 따른
GC·CPU 스파이크를 줄인다. `requests == limits`이므로 미리 잡아도 손해가 없다.
- `**ExitOnOutOfMemoryError**` — OOM 발생 시 JVM이 반쯤 죽은 상태로 살아남아 liveness probe만
통과하는 최악의 상황을 막는다. 즉시 종료해 Kubernetes가 재시작하게 한다.
- `MaxMetaspaceSize=256m` — 상한을 두지 않으면 Metaspace가 컨테이너 limit을 밀어내
heap이 아닌 곳에서 OOMKill이 난다. 원인 파악이 가장 어려운 유형이다.
- `-Xss512k` — Kotlin 코루틴을 쓰면 플랫폼 스레드 수가 줄지만, Tomcat 워커가 여전히 있다.
스택을 절반으로 줄이면 200스레드에서 100MB를 절약한다.

### 원칙 3. CPU limit은 크게 — 스로틀링이 JVM에 특히 해롭다

```yaml
requests: { cpu: 400m }    # 정상 상태 실측 기반 (스케줄링 근거)
limits:   { cpu: 2000m }   # 노드 2 vCPU 전체 — 버스트 허용
```

**이유 1 — CFS 스로틀링과 GC/JIT의 상성이 나쁘다.** CPU limit은 100ms 주기 quota로 구현된다.
JIT 컴파일러 스레드와 G1 GC 워커가 병렬로 돌면 주기 초반에 quota를 소진하고 나머지
수십 ms를 강제 대기한다. 이 대기가 **GC pause에 그대로 더해져** p99 지연이 수백 ms 튄다.
정상 상태 CPU 사용률이 20%인데도 스로틀링이 발생하는 전형적 원인이다.

**이유 2 — `availableProcessors()`가 CPU limit에서 계산된다.** 이게 가장 자주 놓치는 함정이다.


| CPU limit | JVM `availableProcessors()` | 영향                                                                                        |
| --------- | --------------------------- | ----------------------------------------------------------------------------------------- |
| 미설정       | 2 (노드 코어 수)                 | 정상                                                                                        |
| **2000m** | **2**                       | **정상**                                                                                    |
| 1000m     | 1                           | G1 GC 워커 1개, `ForkJoinPool.commonPool` 병렬도 0(=순차), **Kotlin `Dispatchers.Default` 병렬도 1** |
| 500m      | 1                           | 위와 동일 + 심한 스로틀링                                                                           |


`Dispatchers.Default`는 `availableProcessors()` 기반으로 스레드 풀 크기를 정한다.
CPU limit을 1000m로 주면 **코루틴 병렬 처리가 사실상 사라진다.** Kotlin 프로젝트에서
이 함정은 치명적이다.

`limits: 2000m`으로 노드 코어 전부를 허용하면 `availableProcessors()`가 2가 되고 스로틀링도
거의 발생하지 않는다. “노이지 네이버”가 우려되지만 requests 기반 스케줄링이 총합을 통제하고,
파드 수가 20여 개인 규모에서는 실질 위험이 낮다.

**limit을 아예 제거하지 않는 이유** — 6 vCPU 소규모 클러스터에서 폭주하는 파드 하나가
Control Plane(kube-apiserver·etcd)의 CPU까지 빼앗으면 클러스터 전체가 흔들린다.
2000m 상한은 그 최악을 막는 안전선이다. 추가로 namespace에 `LimitRange`를 걸어 누락 시
기본값이 적용되게 한다.

### 원칙 4. 스로틀링을 계측한다

```promql
# 5분간 스로틀 비율 — 0.05(5%) 초과면 CPU limit 상향 검토
rate(container_cpu_cfs_throttled_periods_total{pod=~"aligner-api-.*"}[5m])
  / rate(container_cpu_cfs_periods_total{pod=~"aligner-api-.*"}[5m])
```

이 값과 `jvm_gc_pause_seconds`를 같은 대시보드에 올려 상관을 본다. 자원 튜닝은 추측이 아니라
이 두 그래프로 한다.

## 3.2 시작 시간 단축 — CPU 스파이크의 근본 대응

JVM의 초기 CPU 부하는 클래스 로딩·검증·JIT 워밍업에서 나온다. probe로 시간을 벌어주는 것은
증상 완화이고, 근본 대응은 **시작 자체를 짧게 만드는 것**이다. 6 vCPU 클러스터에서
파드 재시작이 잦으면 시작 스파이크가 다른 파드의 지연으로 전파된다.


| 기법                                         | 효과                                    | 적용                                                                                      |
| ------------------------------------------ | ------------------------------------- | --------------------------------------------------------------------------------------- |
| **CDS (Class Data Sharing)**               | 클래스 로딩·검증 결과를 아카이브로 재사용. 시작 20~40% 단축 | Spring Boot 3.3+ 지원. Paketo buildpack `BP_JVM_CDS_ENABLED=true`로 이미지 빌드 시 자동 생성         |
| **AOT 캐시 (JEP 483, JDK 24+)**              | CDS를 확장해 링킹까지 캐시. 추가 단축               | JDK 25 사용 중이므로 적용 가능. **Spring Boot 4에서 실측 후 채택**                                       |
| Spring AOT 처리                              | 리플렉션·프록시를 빌드 시점에 해석                   | `bootJar` + `springBoot { }` AOT. GraalVM 없이 JVM에서도 이득                                  |
| `AutoCreateSharedArchive`                  | 첫 실행 시 아카이브 자동 생성                     | `-XX:+AutoCreateSharedArchive -XX:SharedArchiveFile=/tmp/app.jsa`. 컨테이너에서는 빌드 시점 생성이 낫다 |
| `~~TieredStopAtLevel=1~~`                  | 시작은 빠르지만 **정상 상태 성능이 크게 저하**          | 상시 API에는 금지. CronJob·Batch에만                                                            |
| `~~spring.main.lazy-initialization=true~~` | 시작은 빠르지만 첫 요청이 느려짐                    | **금지**. readiness 통과 후 실사용자가 지연을 맞는다                                                    |


**권장 이미지 빌드 (Paketo buildpack)**

```kotlin
// build.gradle.kts
tasks.named<BootBuildImage>("bootBuildImage") {
    environment.set(mapOf(
        "BP_JVM_VERSION"     to "25",
        "BP_JVM_CDS_ENABLED" to "true",   // CDS 아카이브를 이미지에 포함
        "BP_SPRING_AOT_ENABLED" to "true"
    ))
}
```

buildpack은 계층 분리(의존성/애플리케이션)를 자동으로 해주므로 이미지 pull 시간도 줄어든다.
`Dockerfile`을 직접 관리하는 것보다 CDS·계층 최적화를 놓칠 위험이 적다.

## 3.3 Probe 전략

### 절대 규칙: liveness와 readiness에 외부 의존성을 넣지 않는다

이것이 Probe 설계에서 가장 중요한 규칙이고 가장 많이 위반된다.

```text
liveness에 DB 헬스체크를 넣으면 →  DB 장애 시 모든 API 파드가 재시작 폭풍에 빠진다.
                                    DB가 돌아와도 파드가 CrashLoopBackOff에서 못 나온다.

readiness에 DB 헬스체크를 넣으면 →  DB 장애 시 모든 파드가 Endpoint에서 제거된다.
                                    503이 아니라 연결 거부가 되어 원인 파악이 어려워지고,
                                    DB가 5초만 끊겨도 전면 장애가 된다.
```

DB 장애는 DB 알림으로 감지한다. Probe의 역할은 “이 파드 프로세스가 살아 있는가 / 트래픽을
받을 준비가 됐는가”뿐이다.

### Spring Boot 설정

```yaml
# application.yaml
management:
  server:
    port: 8081                    # 관리 포트 분리 — Service·LB로 노출하지 않음
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
  endpoint:
    health:
      probes:
        enabled: true             # /health/liveness, /health/readiness 활성화
      group:
        liveness:
          include: livenessState          # ★ 애플리케이션 자체 상태만
        readiness:
          include: readinessState         # ★ db·redis 지시자 제외
      show-details: never
  health:
    db:
      enabled: true               # /actuator/health 에는 노출 (모니터링용)
    redis:
      enabled: true

server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 25s
```

`group.readiness.include: readinessState`를 **명시**하는 것이 핵심이다. 그룹을 정의하지 않은
상태에서 지시자 구성이 바뀌면 DB 헬스가 readiness에 섞여 들어올 수 있다. 명시적으로 못 박는다.

### Deployment Probe

```yaml
containers:
  - name: aligner-api
    ports:
      - { name: http,       containerPort: 8080 }
      - { name: management,  containerPort: 8081 }

    # 1) startupProbe — 시작 지연을 전담 흡수. 이게 있으면 liveness의
    #    initialDelaySeconds를 0으로 둘 수 있다.
    startupProbe:
      httpGet: { path: /actuator/health/liveness, port: management }
      periodSeconds: 5
      failureThreshold: 24          # 최대 120초 허용 (CDS 적용 시 실측 15~25초)
      timeoutSeconds: 3

    # 2) readinessProbe — 트래픽 수용 여부. 실패하면 Endpoint에서만 제거(재시작 안 함)
    readinessProbe:
      httpGet: { path: /actuator/health/readiness, port: management }
      periodSeconds: 5
      failureThreshold: 3           # 15초 연속 실패 시 트래픽 차단
      successThreshold: 1
      timeoutSeconds: 2

    # 3) livenessProbe — 프로세스 데드락 등 회복 불가 상태만 감지. 관대하게.
    livenessProbe:
      httpGet: { path: /actuator/health/liveness, port: management }
      periodSeconds: 10
      failureThreshold: 6           # 60초 연속 실패 시에만 재시작
      timeoutSeconds: 3
```

**설계 의도**


| Probe     | 실패 시 동작              | 튜닝 방향                             |
| --------- | -------------------- | --------------------------------- |
| startup   | 임계 초과 시 재시작          | **관대하게** — 짧으면 정상 앱이 무한 재시작한다     |
| readiness | Endpoint 제거 (재시작 없음) | **민감하게** — 준비 안 된 파드에 트래픽이 가면 5xx |
| liveness  | **컨테이너 재시작**         | **가장 관대하게** — 오탐의 대가가 가장 크다       |


liveness `failureThreshold: 6`(60초)은 일반 권장보다 관대하다. 의도적이다. GC full pause,
일시적 CPU 스로틀링, 노드 I/O 지연으로 3초 타임아웃이 몇 번 실패하는 것은 흔하고, 그때
재시작하면 상황을 악화시킨다. 재시작으로만 고칠 수 있는 상태(데드락)는 60초 뒤에도 여전히
같은 상태다.

**관리 포트 8081 분리** — probe 트래픽이 애플리케이션 스레드 풀·액세스 로그·메트릭을 오염시키지
않는다. Service는 8080만 노출하고 8081은 NetworkPolicy로 kubelet·Alloy에만 허용한다.

## 3.4 Graceful Shutdown — 무중단 배포의 실제 조건

`server.shutdown: graceful` 한 줄로 끝나지 않는다. **Endpoint 전파와 SIGTERM이 경쟁**하기
때문이다.

```text
Pod 삭제 요청
   │
   ├──(A) kubelet → 컨테이너에 SIGTERM  ─────────────► 즉시
   │
   └──(B) EndpointSlice 갱신 → kube-proxy / Traefik 반영 ──► 수백 ms ~ 수 초

A가 B보다 빠르면: 이미 종료를 시작한 파드로 새 요청이 계속 들어와 연결 거부(502/504)
```

Kubernetes는 이 순서를 보장하지 않는다. `**preStop` 훅으로 A를 지연**시켜 해결한다.

```yaml
spec:
  terminationGracePeriodSeconds: 45      # preStop(5s) + 앱 graceful(25s) + 여유
  containers:
    - name: aligner-api
      lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 5"]   # B가 전파될 시간을 벌어준다
```

**타임라인**

```text
t=0     Pod 삭제 → EndpointSlice에서 제거 시작 + preStop 시작
t=0~5   preStop sleep. 앱은 아직 정상 동작 — 진행 중 요청과 신규 요청 모두 처리
t=~1    Traefik이 Endpoint 제거를 반영 → 신규 요청 유입 중단
t=5     preStop 종료 → SIGTERM 전달
t=5     Spring Boot graceful shutdown 시작:
          · 커넥터가 신규 연결 수락 중단
          · 진행 중 요청 완료 대기 (최대 25s)
          · readiness가 OUT_OF_SERVICE로 전환 (Spring Boot가 자동 처리)
t=5~30  진행 중 요청 완료 → ApplicationContext close → HikariCP·코루틴 정리
t=≤45   프로세스 종료 (초과 시 SIGKILL)
```

`terminationGracePeriodSeconds`(45) &gt; `preStop`(5) + `timeout-per-shutdown-phase`(25) 관계를
반드시 지킨다. 역전되면 SIGKILL이 진행 중 요청을 끊는다.

**Kotlin 코루틴 정리**

```kotlin
@Component
class BackgroundScope : DisposableBean {
    private val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)

    // ApplicationContext close 시 호출 — timeout-per-shutdown-phase 안에서 끝나야 한다
    override fun destroy() = runBlocking {
        withTimeoutOrNull(20_000) { job.cancelAndJoin() }
        Unit
    }
}
```

`GlobalScope`를 쓰면 shutdown 시점에 취소할 방법이 없어 SIGKILL까지 살아남는다. 구조화된
동시성을 유지해야 graceful shutdown이 성립한다.

**Spring Data JDBC / HikariCP**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10        # ★ 산정 근거 아래
      minimum-idle: 5
      connection-timeout: 3000     # 3초 — 무한 대기 금지
      max-lifetime: 900000         # 15분. CNPG failover 후 stale 연결을 순환시킨다
      keepalive-time: 300000       # 5분
      validation-timeout: 2000
```

**풀 크기 산정** — PostgreSQL `max_connections: 120`(§2.5.2)을 넘지 않아야 한다.

```text
API 3 replica × 10 = 30
Batch/Worker 1 × 5  =  5
CNPG 내부(복제·모니터링·백업)  ≈ 15
운영자 수동 접속 여유          ≈ 10
─────────────────────────────────
합계 60  <  max_connections 120   ✅ HPA로 API가 4~5개까지 늘어도 안전
```

풀을 크게 잡는 것이 성능에 유리하다는 직관은 틀렸다. 2 vCPU 노드에서 동시 실행 가능한 쿼리는
소수이고, 풀이 크면 DB 쪽 컨텍스트 스위칭과 메모리(`work_mem` × 연결 수)만 늘어난다.

## 3.5 배치·스케줄링과 클러스터 자원 검증

### 워크로드 배치

```yaml
# 노드 분산 — 3노드에 2~3 replica
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: kubernetes.io/hostname
    whenUnsatisfiable: DoNotSchedule       # 같은 노드에 몰리는 것을 금지
    labelSelector:
      matchLabels: { app: aligner-api }

# 자발적 중단(노드 drain·업그레이드)에서 최소 가용성 보장
---
apiVersion: policy/v1
kind: PodDisruptionBudget
spec:
  maxUnavailable: 1                        # minAvailable보다 replica 변동에 견고
  selector:
    matchLabels: { app: aligner-api }

# 자원 압박 시 축출 순서
---
priorityClassName: aligner-api-high        # 시스템 > API > Batch
```

`PDB`에 `minAvailable` 대신 `**maxUnavailable: 1**` 을 쓴다. HPA로 replica가 2↔5로 변할 때
`minAvailable: 2`는 replica 2에서 drain을 완전히 막아버리는 반면 `maxUnavailable: 1`은
항상 “한 번에 하나씩”으로 동작한다.

### HPA — JVM에서 메모리 기반 스케일링은 쓰지 않는다

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 2
  maxReplicas: 4                     # 노드 3개 · 자원 여유 기준 상한
  metrics:
    - type: Resource
      resource:
        name: cpu                    # ★ CPU만 사용
        target: { type: Utilization, averageUtilization: 70 }
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 30
    scaleDown:
      stabilizationWindowSeconds: 300   # 5분 — JVM 워밍업 비용 때문에 보수적으로
```

**메모리 메트릭을 쓰지 않는 이유** — JVM RSS는 부하가 빠져도 내려오지 않으므로 메모리 기반
HPA는 **스케일아웃만 하고 스케일인을 하지 않는다.** replica가 상한에 붙어 고정되고
자원이 잠긴다.

**CPU 기준 70%의 주의점** — HPA는 `requests.cpu`(400m) 대비 사용률로 계산한다. 즉 280m를
넘으면 스케일아웃한다. JIT 워밍업 구간에서 CPU가 일시적으로 튀어 불필요한 스케일아웃이
발생할 수 있으므로 `scaleUp.stabilizationWindowSeconds: 30`으로 완충한다. 정확한 스케일링
지표는 요청률·큐 길이지만 Prometheus Adapter가 필요하므로 이 규모에서는 CPU로 시작하고
Phase 3 부하 테스트 결과로 임계를 조정한다.

### 클러스터 전체 자원 검증 — 노드 1대 장애를 견디는가

이 계산이 §1.2에서 `2/8 × 3`을 선택한 최종 근거다.

**노드당 allocatable**

```text
물리 메모리                      8192Mi
- system-reserved                 512Mi
- kube-reserved                   512Mi
- eviction-hard threshold         300Mi
- K3s server 프로세스 실사용    ~700Mi   (apiserver·etcd·scheduler·cm·kubelet·containerd)
──────────────────────────────────────
파드 배치 가용                  ~6100Mi  (≈ 6.0Gi)

3노드 총 가용        ≈ 18.0Gi
1노드 장애 시 가용   ≈ 12.0Gi   ← 이 값이 상한선이다
```

**requests 총합**


| 워크로드                                         | 개수  | CPU req | Mem req | CPU 합     | Mem 합      |
| -------------------------------------------- | --- | ------- | ------- | --------- | ---------- |
| Traefik                                      | 3   | 100m    | 128Mi   | 300m      | 384Mi      |
| CoreDNS                                      | 2   | 100m    | 170Mi   | 200m      | 340Mi      |
| metrics-server                               | 1   | 100m    | 200Mi   | 100m      | 200Mi      |
| local-path-provisioner                       | 1   | 50m     | 64Mi    | 50m       | 64Mi       |
| cert-manager (3 pods)                        | 3   | 50m     | 96Mi    | 150m      | 288Mi      |
| sealed-secrets-controller                    | 1   | 50m     | 64Mi    | 50m       | 64Mi       |
| **Argo CD** (server·repo·app-ctrl·redis·dex) | 5   | 100m    | 128Mi   | 500m      | 640Mi      |
| Grafana Alloy (DaemonSet)                    | 3   | 100m    | 256Mi   | 300m      | 768Mi      |
| kube-state-metrics                           | 1   | 50m     | 128Mi   | 50m       | 128Mi      |
| CNPG operator                                | 1   | 100m    | 200Mi   | 100m      | 200Mi      |
| **PostgreSQL (CNPG)**                        | 2   | 400m    | 2048Mi  | 800m      | 4096Mi     |
| Redis                                        | 1   | 100m    | 512Mi   | 100m      | 512Mi      |
| **Aligner API**                              | 3   | 400m    | 1536Mi  | 1200m     | 4608Mi     |
| Worker / Batch                               | 1   | 250m    | 1024Mi  | 250m      | 1024Mi     |
| **합계 (약 28 pods)**                           |     |         |         | **4150m** | **13.0Gi** |


**검증 결과**


| 항목                 | 값                         | 판정              |
| ------------------ | ------------------------- | --------------- |
| CPU requests 총합    | 4150m / 6000m (69%)       | ✅ 여유 있음         |
| Memory requests 총합 | 13.0Gi / 18.0Gi (72%)     | ✅ 여유 있음         |
| **1노드 장애 시 가용**    | 12.0Gi vs requests 13.0Gi | ⚠️ **1.0Gi 부족** |


1노드 장애 시 정확히 1Gi가 부족하다. 이 상황을 **설계된 축소 운전(degraded mode)** 으로
미리 정의한다. 자원 부족 시 무엇이 먼저 밀려날지 우연에 맡기지 않는다.

```yaml
# PriorityClass 정의 — 축출 순서를 명시적으로 통제
system-node-critical  (K3s 기본)   : CoreDNS, Traefik, Alloy
aligner-critical      value: 1000  : PostgreSQL, Redis, Argo CD app-controller
aligner-high          value: 500   : Aligner API
aligner-normal        value: 100   : Worker / Batch
aligner-low           value: 10    : Argo CD server·dex(UI), kube-state-metrics
```

1노드 장애 시 축소 순서:

1. `aligner-low` 축출 → Argo CD server·dex(UI), kube-state-metrics 중단 (약 0.4Gi 확보)
2. 그래도 부족하면 `aligner-normal` → Worker/Batch 중단 (1.0Gi 확보)
3. `**aligner-high`(API)와 `aligner-critical`(DB·Argo CD app-controller)은 유지** → 서비스 지속

이 순서로 1노드 장애 시 **API 3 replica + PostgreSQL HA를 모두 유지**한 채 12.0Gi 안에
들어간다(13.0 − 0.4 − 1.0 = **11.6Gi &lt; 12.0Gi**). 배포 파이프라인(Argo CD UI)과 배치 작업을
내주고 서비스를 지킨다는 우선순위다. 추가로 HPA `minReplicas: 2`가 동작하면 API가 2개로
줄어 1.5Gi가 더 확보되므로 실제 여유는 더 크다.

주의: 축출은 `requests`를 초과한 사용량이 아니라 **노드 메모리 압박**에 의해 트리거된다.
`kubelet`의 `eviction-hard`(300Mi)와 PriorityClass가 이 동작의 전제이므로 §2.1의
`kubelet-arg` 설정이 빠지면 이 시나리오가 성립하지 않는다.

**Aligner API replica 3의 근거** — `topologySpreadConstraints`로 노드마다 1개씩 배치되므로
어느 노드가 죽어도 2개가 남는다. replica 2면 죽은 노드에 1개가 있어 순간적으로 1개만 남는다.
3노드 클러스터에서 replica 3은 낭비가 아니라 최소값이다.

## 3.6 Spring Boot 컨테이너 체크리스트

배포 전 확인 항목이다. 하나라도 빠지면 운영 중 사고로 나타난다.


| #   | 항목                                                                                    | 확인  |
| --- | ------------------------------------------------------------------------------------- | --- |
| 1   | `requests.memory == limits.memory`                                                    | 필수  |
| 2   | `MaxRAMPercentage` 사용, `-Xmx` 고정값 없음                                                  | 필수  |
| 3   | `limits.cpu ≥ 2000m` (`availableProcessors()` = 2 확보)                                 | 필수  |
| 4   | `MaxMetaspaceSize` 상한 설정                                                              | 필수  |
| 5   | `ExitOnOutOfMemoryError` 설정                                                           | 필수  |
| 6   | startup / readiness / liveness 3종 모두 설정                                               | 필수  |
| 7   | liveness·readiness에 DB·Redis 헬스체크 **없음**                                              | 필수  |
| 8   | 관리 포트(8081) 분리, Service 미노출                                                           | 필수  |
| 9   | `server.shutdown: graceful` + `preStop sleep 5` + `terminationGracePeriodSeconds: 45` | 필수  |
| 10  | HikariCP 풀 크기 × replica ≤ PG `max_connections` 여유                                     | 필수  |
| 11  | HPA 메트릭에 memory 없음                                                                    | 필수  |
| 12  | `topologySpreadConstraints` + PDB `maxUnavailable: 1`                                 | 필수  |
| 13  | `runAsNonRoot` + `readOnlyRootFilesystem` (+ heapdump용 `/tmp` emptyDir)               | 필수  |
| 14  | 이미지 digest 고정, `latest` 없음                                                            | 필수  |
| 15  | CDS 활성화 이미지                                                                           | 권장  |
| 16  | `PriorityClass` 지정                                                                    | 권장  |
| 17  | `lazy-initialization` / `TieredStopAtLevel=1` 미사용                                     | 필수  |


`readOnlyRootFilesystem: true`를 쓰면 heap dump 경로가 쓰기 불가가 되므로 `/tmp`를
`emptyDir`로 마운트해야 한다. 이 조합을 놓치면 OOM 시 dump가 안 남아 원인 분석이 불가능해진다.

---

# 세션 4. 9개월 구축·운영 로드맵 (Phase 1~4)

각 Phase에 **완료 조건(DoD)** 과 **검증 명령**을 붙인다. 검증하지 않은 항목은 완료로 세지 않는다.
크레딧 만료가 확정된 프로젝트에서 “나중에 하겠다”는 사실상 “하지 않는다”이므로, 백업·복구
검증을 Phase 4가 아니라 **Phase 2에 배치**한 것이 원본 계획과의 주요 차이다.

## Phase 0. 착수 전 (D-7 ~ D-0, 크레딧 미소진)


| 작업                                                               | 산출물     |
| ---------------------------------------------------------------- | ------- |
| §1.6 확인 항목 7개를 가비아에 문의해 서면 답변 확보                                 | 답변 기록   |
| 요금 계산기에서 채택안 견적 재확인 (단가 변동 확인)                                   | 견적 스크린샷 |
| 도메인 확보 및 DNS 준비 (`aligner.example.com`, `k8s-api.*`, `argocd.*`) | DNS 존   |
| AWS S3 버킷 + 백업 전용 IAM 사용자 생성 (최소 권한)                             | 버킷·키    |
| GitHub 저장소 준비 (앱 저장소 / **GitOps 저장소 분리**)                        | 저장소 2개  |
| Grafana Cloud 무료 계정 생성, 토큰 발급                                    | 토큰      |
| Tailscale 조직 생성 (팀 계정)                                           | 초대 완료   |


**GitOps 저장소를 앱 저장소와 분리하는 이유** — 애플리케이션 CI가 이미지 digest를 커밋할 때
앱 저장소에 커밋하면 CI가 다시 트리거되는 루프가 생긴다. 또 인프라 변경 이력과 애플리케이션
변경 이력의 리뷰 기준이 다르다(`AGENTS.md` §5의 “의존성 변경은 최우선 리뷰 대상”과 같은 맥락).

**DoD** — 확인 항목 3(사설망 무과금)·4(S3 트래픽 분류) 답변 확보. 4번이 “해외”면 백업 대상을
가비아 오브젝트 스토리지로 바꾼 뒤 Phase 1에 진입한다.

---

## Phase 1. 인프라 · 클러스터 · 보안 기준선 (1~2개월차)

크레딧 소진 시작. 목표는 **“의도적으로 노드를 죽여도 서비스가 유지되는 클러스터”** 다.

### 1개월차


| #   | 작업                                                               | 세부                                                       |
| --- | ---------------------------------------------------------------- | -------------------------------------------------------- |
| 1   | Terraform으로 VPC·서브넷·라우터·보안그룹·VM 3대·블록 스토리지·공인 IP·LB 프로비저닝        | 콘솔 수동 생성 금지. Phase 4 재구축의 전제                             |
| 2   | 노드 초기화 (Ansible 또는 cloud-init)                                   | 커널 파라미터, Data SSD 마운트(`/mnt/data`), 시간 동기화, Tailscale 설치 |
| 3   | 보안그룹 잠금 (§1.5 표)                                                 | 22는 Tailscale 대역만, 6443은 관리자·노드 대역만                      |
| 4   | K3s server 3노드 embedded etcd HA 부트스트랩 (§2.1 config)              | `traefik`·`servicelb` disable, `data-dir`을 Data SSD      |
| 5   | LB 리스너 3개 구성 (443→30443, 80→30080, 6443→6443)                    | 헬스 체크 확인                                                 |
| 6   | Traefik Helm 배포 (3 replica, `externalTrafficPolicy: Local`, PDB) | 아직 GitOps 없이 수동 — 다음 달 Argo CD가 인수                       |
| 7   | cert-manager + Let's Encrypt ClusterIssuer, 첫 인증서 발급             | staging → production 순서                                  |
| 8   | etcd snapshot S3 업로드 확인 (6시간 주기)                                 | K3s 내장 기능                                                |


### 2개월차


| #   | 작업                                                                | 세부                                     |
| --- | ----------------------------------------------------------------- | -------------------------------------- |
| 9   | Pod Security Admission `restricted` 적용                            | 애플리케이션 namespace                       |
| 10  | **NetworkPolicy default-deny + 명시적 허용** (§2.3)                    | 원본 대비 앞당긴 항목                           |
| 11  | Sealed Secrets 컨트롤러 설치 + **봉인 키 오프라인 백업**                         | Phase 4 이관의 전제                         |
| 12  | Argo CD 설치, app-of-apps 구성, Traefik·cert-manager를 GitOps로 인수      | 수동 리소스를 Git으로 이관                       |
| 13  | Argo CD 접근 통제: `admin` 비활성화 + GitHub OAuth + RBAC 또는 Tailscale 전용 | 인증 없는 노출 금지                            |
| 14  | **장애 훈련 #1 — 노드 1대 강제 정지**                                        | etcd quorum·API·Traefik·Endpoint 반응 관측 |
| 15  | **복구 훈련 #1 — etcd snapshot 복구**                                   | 3노드 재조립 절차 문서화                         |


### DoD 및 검증

```bash
# 1. 3노드 Ready, 대칭 확인
kubectl get nodes -o wide
kubectl get nodes -o json | jq '.items[].status.allocatable'

# 2. etcd 멤버 3개, 모두 정상
sudo k3s etcd-snapshot ls
kubectl -n kube-system get pods            # etcd는 프로세스이므로 K3s 로그로 확인
sudo journalctl -u k3s | grep -i "etcd.*member"

# 3. 노드 1대 정지 후에도 API·서비스 응답 (훈련 #1)
#    별도 노드에서 실행
while true; do curl -s -o /dev/null -w "%{http_code} " https://aligner.example.com/health; sleep 1; done
# → 정지 직후 일부 실패 후 회복되어야 한다. 지속 실패면 설계 문제.

# 4. NetworkPolicy 동작 (허용되지 않은 통신 차단)
kubectl run netpol-test --rm -it --image=busybox --restart=Never -- \
  sh -c "wget -qO- --timeout=3 http://aligner-db-rw.database:5432 || echo BLOCKED_OK"

# 5. TLS 인증서 발급
kubectl get certificate -A
echo | openssl s_client -connect aligner.example.com:443 2>/dev/null | openssl x509 -noout -dates

# 6. Argo CD 동기화 상태
argocd app list                            # 전부 Synced / Healthy
```


| DoD                                            | 기준  |
| ---------------------------------------------- | --- |
| 노드 3대 Ready, allocatable ≈ 6.0Gi/노드            | 필수  |
| 노드 1대 정지 시 API 5분 내 정상, etcd quorum 유지         | 필수  |
| etcd snapshot이 S3에 6시간 주기로 적재                  | 필수  |
| snapshot으로 클러스터 복구 절차를 **1회 실제 수행**하고 소요 시간 기록 | 필수  |
| default-deny NetworkPolicy 하에 애플리케이션 통신 정상     | 필수  |
| Sealed Secrets 봉인 키 오프라인 백업 완료                 | 필수  |
| Terraform `plan`이 no-change (실제 상태 = 코드)       | 필수  |
| Argo CD가 platform 전체를 관리, 수동 리소스 0             | 필수  |


---

## Phase 2. 애플리케이션 · 데이터베이스 · 관측성 (3~4개월차)

목표는 **“Aligner API가 관측·백업되는 상태로 실제 운영되는 것”** 이다.

### 3개월차


| #   | 작업                                                            | 세부                                                                                                       |
| --- | ------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| 1   | CI 파이프라인 완성 (GitHub Actions)                                  | `build` → `ktlintCheck` → `integrationTest`(TestContainers) → 이미지(CDS) → Trivy → GHCR → GitOps digest 커밋 |
| 2   | CloudNativePG operator 설치, `aligner-db` Cluster 2 instance 배포 | anti-affinity로 다른 노드 강제                                                                                  |
| 3   | Liquibase changelog 적용 경로 확정                                  | 도메인별 changelog, 애플리케이션 시작 시 또는 Job                                                                       |
| 4   | Barman Cloud Plugin → S3 WAL 아카이빙 + 주간 basebackup             | 아카이빙 성공 로그 확인                                                                                            |
| 5   | Aligner API 배포 (§3 전체 설정 적용)                                  | 3 replica, probe 3종, graceful shutdown, PDB, topologySpread                                              |
| 6   | Redis 배포 (캐시 용도, 유실 허용)                                       | local-path PVC                                                                                           |
| 7   | Grafana Alloy 배포, Grafana Cloud 연결                            | metrics + logs + traces                                                                                  |
| 8   | 카디널리티 제어 relabel 규칙 적용 (§2.7)                                 | Free tier 한도 내 유지 확인                                                                                     |


### 4개월차


| #   | 작업                                    | 세부                                                                     |
| --- | ------------------------------------- | ---------------------------------------------------------------------- |
| 9   | 대시보드 구축                               | JVM heap·GC, **CFS throttle**, HTTP p95/p99, HikariCP, PG 복제 지연, 노드 자원 |
| 10  | 알림 규칙 (Grafana Cloud → Slack/Discord) | 아래 표                                                                   |
| 11  | 외부 프로빙(Synthetic) 설정                  | 클러스터 전체 다운을 외부에서 감지                                                    |
| 12  | **복구 훈련 #2 — PostgreSQL PITR**        | 임의 시점 복구, 소요 시간 기록                                                     |
| 13  | **failover 훈련 — CNPG primary 파드 삭제**  | switchover 시간, 앱 재연결 확인                                                |
| 14  | 자원 실측 기반 requests/limits 1차 조정        | 추측값 → 실측값                                                              |
| 15  | HPA 적용 및 동작 확인                        | CPU 70%, min 2 / max 4                                                 |


**알림 규칙 (최소 세트)**


| 알림                  | 조건                                | 심각도      |
| ------------------- | --------------------------------- | -------- |
| 서비스 다운              | 외부 프로빙 실패 2회 연속                   | Critical |
| 노드 NotReady         | 3분 이상                             | Critical |
| etcd 멤버 이탈          | 1분 이상                             | Critical |
| PG 복제 지연            | `pg_replication_lag > 60s`        | Warning  |
| PG 백업 실패            | WAL 아카이빙 실패 또는 basebackup 미수행 24h | Critical |
| 파드 CrashLoopBackOff | 5분 이상                             | Warning  |
| OOMKilled 발생        | 즉시                                | Warning  |
| CPU 스로틀 비율          | 5분 평균 &gt; 5%                     | Warning  |
| 메모리 requests 총합     | allocatable(1노드 장애 기준) 초과         | Warning  |
| 인증서 만료 임박           | 14일 이내                            | Warning  |
| 디스크 사용률             | Data SSD &gt; 75%                 | Warning  |


### DoD 및 검증

```bash
# 1. PG HA 상태 — primary/standby가 다른 노드에 있는지
kubectl -n database get pods -o wide -l cnpg.io/cluster=aligner-db
kubectl -n database get cluster aligner-db -o jsonpath='{.status.instancesStatus}' | jq

# 2. 백업·WAL 아카이빙 동작
kubectl -n database get cluster aligner-db \
  -o jsonpath='{.status.lastSuccessfulBackup}{"\n"}{.status.firstRecoverabilityPoint}{"\n"}'
aws s3 ls s3://aligner-backup/aligner-db/wals/ --recursive | tail -5

# 3. PITR 복구 검증 (훈련 #2) — 별도 Cluster로 복구, 운영 DB 건드리지 않음
#    recovery target time 지정 후 데이터 시점 일치 확인

# 4. failover (훈련 #3)
kubectl -n database delete pod aligner-db-1        # primary 삭제
kubectl -n database get cluster aligner-db -w      # targetPrimary 전환 관측
# 앱 로그에서 HikariCP 재연결 확인. 무중단이 아니라 '빠른 회복'이 목표.

# 5. Graceful shutdown 검증 — 롤링 업데이트 중 5xx 0건
kubectl rollout restart deployment/aligner-api -n aligner
# 동시에 부하 생성:
hey -z 90s -c 20 https://aligner.example.com/api/body-parts
# → non-2xx 응답이 0이어야 한다. 있으면 preStop·terminationGracePeriod 재조정.

# 6. 자원 실측
kubectl top pods -A --sort-by=memory
kubectl top nodes
```


| DoD                                               | 기준  |
| ------------------------------------------------- | --- |
| Aligner API 3 replica 정상, p95 목표 이내               | 필수  |
| **롤링 업데이트 중 5xx 0건**                              | 필수  |
| PG primary/standby가 서로 다른 노드, 복제 지연 &lt; 5s       | 필수  |
| WAL 아카이빙 연속 동작, 주간 basebackup 성공                  | 필수  |
| **PITR 복구를 실제 수행하고 RTO 실측치 기록**                   | 필수  |
| primary 삭제 시 자동 failover, 앱 자동 재연결                | 필수  |
| Grafana Cloud에 metrics·logs·traces 모두 수집, 무료 한도 내 | 필수  |
| 알림 11종 설정 및 각 1회 실발화 테스트                          | 필수  |
| requests/limits가 실측 기반으로 갱신됨                      | 필수  |


---

## Phase 3. 성능 · 부하 · 축소 운전 검증 (5~6개월차)

목표는 **“한계와 축소 운전 동작을 숫자로 아는 것”** 이다. Phase 1~2는 “동작한다”를,
Phase 3은 “어디까지 동작하고 넘어가면 어떻게 되는가”를 확인한다.


| #   | 작업                                                                      | 세부                                         |
| --- | ----------------------------------------------------------------------- | ------------------------------------------ |
| 1   | 부하 테스트 (k6 또는 Gatling) — **클러스터 외부에서 생성**                               | 클러스터 안에서 부하를 만들면 자원을 경합해 결과가 오염된다          |
| 2   | 시나리오: 핵심 루프 (`BodyPart` → `Screening` → `Cause` → `Course` → `Session`) | 실제 사용 경로. 단일 엔드포인트 테스트는 의미가 적다             |
| 3   | JVM 워밍업 곡선 측정                                                           | 시작 후 몇 초에 p99가 안정되는가 → HPA `scaleUp` 튜닝 근거 |
| 4   | CDS·AOT 캐시 적용 전후 시작 시간 A/B 측정                                           | 실측으로 채택 여부 결정 (§3.2)                       |
| 5   | HPA 임계 조정                                                               | 부하 곡선 기반                                   |
| 6   | 커넥션 풀 포화 지점 확인                                                          | `hikaricp_connections_pending` 관측          |
| 7   | **축소 운전 훈련 — PriorityClass 축출 순서 검증** (§3.5)                            | 노드 1대 정지 후 무엇이 먼저 죽는지 실제 확인                |
| 8   | **장애 훈련 #4 — 노드 2대 동시 정지**                                              | etcd quorum 상실 상태 관측. 복구 절차 확인             |
| 9   | **장애 훈련 #5 — 노드 1대 완전 재설치**                                             | Terraform destroy/apply 후 클러스터 재합류         |
| 10  | 로그·메트릭 보관 정책 점검                                                         | Grafana Cloud 한도 소진 속도 확인                  |
| 11  | **K3s 마이너 업그레이드 리허설**                                                   | 노드 순차 업그레이드, PDB 동작 확인                     |
| 12  | Data SSD 사용률 점검 및 필요 시 증설                                               | 10GB 단위                                    |
| 13  | 런북(runbook) 작성                                                          | 알림별 대응 절차. 새벽에 당황하지 않기 위한 문서               |


**훈련 #4(노드 2대 정지)를 반드시 하는 이유** — 이 클러스터의 HA 경계가 “1노드 장애”라는 것을
문서로 아는 것과 실제로 겪는 것은 다르다. quorum을 잃으면 API가 read-only도 아니라 응답 자체를
멈춘다. 이 상태에서 `--cluster-reset`으로 단일 노드 복구 후 나머지를 재합류시키는 절차는
연습 없이 실전에서 하면 반드시 실수한다.

```bash
# 훈련 #4 복구 절차 (사전 연습 필수)
# 살아있는 노드에서:
sudo systemctl stop k3s
sudo k3s server --cluster-reset \
  --cluster-reset-restore-path=/mnt/data/k3s/server/db/snapshots/<snapshot>
sudo systemctl start k3s
# 나머지 노드: data-dir 삭제 후 재조인
```

### DoD


| DoD                               | 기준  |
| --------------------------------- | --- |
| 목표 처리량에서 p95·p99 지연 실측치 확보        | 필수  |
| 자원 한계(최초 병목 지점)와 그때의 증상 기록        | 필수  |
| PriorityClass 축출 순서가 설계대로 동작함을 확인 | 필수  |
| 노드 2대 정지 → 복구 절차 실제 수행 및 RTO 기록   | 필수  |
| 노드 1대 재설치 → Terraform으로 재합류 성공    | 필수  |
| K3s 업그레이드 무중단 수행 (5xx 0건)         | 필수  |
| 알림별 런북 문서 완성                      | 필수  |
| CDS·AOT 적용 여부를 실측 근거로 결정          | 권장  |


---

## Phase 4. DR · 이관 리허설 · 종료 계획 (7~9개월차)

목표는 **“크레딧이 끝나도 이 서비스를 다른 곳에서 다시 세울 수 있음을 증명하는 것”** 이다.
잔여 크레딧으로 임시 4번째 노드를 띄우는 시점이 여기다(§1.3).

### 7개월차 — 전체 복구 리허설 (신규 클러스터)


| #   | 작업                                                             |
| --- | -------------------------------------------------------------- |
| 1   | 잔여 크레딧으로 임시 노드 확보 (`2/8` × 1, 또는 기존 노드 재활용)                    |
| 2   | **Terraform으로 완전히 새로운 클러스터 프로비저닝** (기존 클러스터 무중단 유지)            |
| 3   | Sealed Secrets 봉인 키 복원 → Git의 모든 `SealedSecret`이 그대로 동작하는지 확인  |
| 4   | Argo CD 부트스트랩 → app-of-apps 동기화만으로 전체 플랫폼 재구성                  |
| 5   | S3 basebackup + WAL로 PostgreSQL 복구 (CNPG `bootstrap.recovery`) |
| 6   | 데이터 정합성 검증 (행 수, 최신 `Session` 시각, 체크섬)                         |
| 7   | **전체 복구 RTO·RPO 실측** 및 원본 목표(§2.5.2 표)와 비교                     |


**이 리허설이 모든 앞선 설계의 검증**이다. Terraform·GitOps·Sealed Secrets 키 백업·S3 백업 중
하나라도 빠져 있으면 여기서 실패한다. 실패는 성과다 — 크레딧이 살아 있는 동안 발견한 것이므로.

### 8개월차 — Cilium·Longhorn 검증과 최적화


| #   | 작업                                                                      |
| --- | ----------------------------------------------------------------------- |
| 8   | 신규 클러스터에 **Cilium** 설치 (`kube-proxy replacement` + Hubble) — §2.3 유보 항목 |
| 9   | Flannel 대비 리소스·지연 비교, `CiliumNetworkPolicy` L7 정책 실험                    |
| 10  | **Longhorn** replica 2로 비핵심 데이터 검증 — 2 vCPU에서의 실제 CPU 부하 측정             |
| 11  | 두 실험 결과를 문서화하고 “다음 클러스터에서 채택/미채택” 결정 기록                                 |
| 12  | K3s 메이저 업그레이드 리허설 (신규 클러스터에서 먼저)                                        |
| 13  | 비용 실적 정산 — 예측 대비 실제 청구액 차이 분석                                           |


### 9개월차 — 종료 · 이관 결정


| #   | 작업                                                                 |
| --- | ------------------------------------------------------------------ |
| 14  | 최종 백업: PG 논리 덤프(`pg_dump`) + basebackup + etcd snapshot → S3 다중 사본 |
| 15  | 컨테이너 이미지 GHCR 보존 확인 (digest 목록 기록)                                 |
| 16  | 이관 시나리오 결정: (a) 유료 전환 지속 (b) 타 클라우드 이전 (c) 종료 후 데이터 보관             |
| 17  | 이관 시 예상 비용 산정 (동일 사양 기준 타 CSP 비교)                                  |
| 18  | **9개월 회고 문서** 작성                                                   |
| 19  | 크레딧 잔액 확인 및 리소스 삭제 순서 계획 (LB → VM → 스토리지 → IP)                     |
| 20  | 삭제 전 최종 스냅샷 및 S3 사본 무결성 검증                                         |


**회고 문서에 반드시 담을 것**

- 훈련별 실측 RTO/RPO와 목표치 차이
- 자원 실측치와 초기 산정치의 차이 (다음 설계의 근거)
- 이 설계에서 틀렸던 판단 (예: `2/8`이 CPU 병목이었는가, PG 2 instance가 과했는가)
- Cilium·Longhorn 검증 결과와 채택 판단
- 다시 한다면 바꿀 것

### DoD


| DoD                                                           | 기준  |
| ------------------------------------------------------------- | --- |
| **신규 클러스터에서 전체 복구 성공** (Terraform → Argo CD → PG 복구 → 데이터 검증) | 필수  |
| 전체 복구 RTO 실측 및 목표 대비 평가                                       | 필수  |
| Sealed Secrets 키 복원만으로 전체 시크릿 동작 확인                           | 필수  |
| 최종 백업 3중 사본 및 복원 가능성 검증                                       | 필수  |
| 이관/종료 결정과 비용 산정 완료                                            | 필수  |
| 회고 문서 작성                                                      | 필수  |
| Cilium·Longhorn 검증 결과 문서화                                     | 권장  |


---

## 훈련 일정 요약


| 훈련    | 내용                                          | 시점        |
| ----- | ------------------------------------------- | --------- |
| #1    | 노드 1대 강제 정지                                 | 2개월차      |
| 복구 #1 | etcd snapshot 복구                            | 2개월차      |
| 복구 #2 | PostgreSQL PITR                             | 4개월차      |
| #3    | CNPG primary failover                       | 4개월차      |
| #4    | 노드 2대 정지 (quorum 상실) + `--cluster-reset` 복구 | 6개월차      |
| #5    | 노드 1대 완전 재설치 후 재합류                          | 6개월차      |
| #6    | K3s 무중단 업그레이드                               | 6개월차      |
| #7    | **신규 클러스터 전체 복구 (DR)**                      | 7개월차      |
| 정례    | 월 1회 백업 복구 검증                               | 2개월차부터 매월 |


---

# 최종 구성 요약

```text
[ 인프라 ]
Cloud            : 가비아 클라우드 Gen2 (VPC)
Cluster          : 3-node Converged HA (Control Plane + etcd + Worker 대칭)
Node             : standard 2 vCPU / 8GB × 3        (총 6 vCPU / 24GB)
Root SSD         : 50GB × 3   (VM 요금 포함)
Data SSD         : 60GB × 3 = 180GB  ← K3s data-dir · etcd · local-path PV
Load Balancer    : Gabia External LB (Small) — 443/80/6443
공인 IP          : 4개 (노드 3 아웃바운드 + LB 1)
관리 접근        : Tailscale 오버레이 (22·6443 인터넷 미노출)

[ 플랫폼 ]
Kubernetes       : K3s (embedded etcd ×3, 2순위 RKE2)
Runtime          : containerd
CNI              : Flannel VXLAN + 표준 NetworkPolicy (default-deny)
Service Proxy    : kube-proxy
Ingress          : Traefik × 3 (K3s 번들 해제 → Argo CD가 Helm으로 관리)
TLS              : cert-manager + Let's Encrypt
Storage          : local-path Provisioner (Longhorn 미도입)

[ 배포 ]
CI               : GitHub Actions (build · ktlint · integrationTest · CDS 이미지 · Trivy · GHCR)
GitOps           : Argo CD (app-of-apps, automated sync · prune · selfHeal)   ← 원본 Flux에서 변경
Manifest         : Kustomize (자체 앱) + Helm (외부 솔루션)
Image            : digest 고정, latest 금지
Secret           : Sealed Secrets                                            ← 원본 SOPS+age에서 변경

[ 데이터 ]
Database         : CloudNativePG 2 instance (primary + standby, anti-affinity) ← 원본 단일에서 변경
Backup           : Barman Cloud → S3 (WAL 연속 + 주간 basebackup, PITR)
etcd Backup      : K3s 내장 snapshot → S3 (6시간 주기, 로컬 28개)
Cache            : Redis 1 (유실 허용)

[ 관측 ]
Collector        : Grafana Alloy DaemonSet ×3 (metrics · logs · traces)
Backend          : Grafana Cloud (클러스터 외부 — 장애 시에도 생존)
클러스터 내부    : metrics-server, kube-state-metrics 만
Alerting         : Grafana Cloud + 외부 Synthetic 프로빙 → Slack/Discord

[ 비용 ]
월 예상액        : 301,895원 (VAT 포함)
9개월 기본 운영  : 2,717,055원
잔여 크레딧      : 282,945원 → Phase 4 이관 리허설 노드(172,150원) + 변동비 예비
예상 총 소진     : 약 2,939,000원 (98.0%)
```

## 원본 설계안 대비 변경 요약


| #   | 변경            | 원본               | 본안                                     | 원인                                       |
| --- | ------------- | ---------------- | -------------------------------------- | ---------------------------------------- |
| 1   | 노드 사양         | `2/4` × 3 (12GB) | `**2/8` × 3 (24GB)**                   | 월 예산 25만 → 33만 원                         |
| 2   | 기간            | 12개월             | **9개월**                                | 조건 변경                                    |
| 3   | GitOps        | Flux CD          | **Argo CD**                            | 프로젝트 README 정본 일치 + 팀 5인 가시성 + 메모리 제약 해소 |
| 4   | Secret        | SOPS + age       | **Sealed Secrets**                     | Argo CD는 SOPS 내장 미지원 (3번의 귀결)            |
| 5   | Database      | 단일 Primary + 백업  | **CNPG 2 instance + PITR**             | 메모리 24GB 확보                              |
| 6   | Ingress 관리    | K3s 번들 Traefik   | **번들 해제 → Argo CD Helm**               | GitOps 일관성 (버전·설정을 Git에)                 |
| 7   | etcd 배치       | 기본 경로            | **Data SSD 분리**                        | fsync I/O 격리                             |
| 8   | etcd 백업       | 로컬 + 주 1회 수동 S3  | **K3s 내장 S3 직접 업로드 6시간**               | 내장 기능 활용, CronJob 제거                     |
| 9   | NetworkPolicy | “초기 필수 보안”       | **Phase 1 DoD, default-deny**          | 시점 명시                                    |
| 10  | 관리 접근         | 고정 IP 화이트리스트     | **Tailscale 오버레이**                     | 유동 IP 대응 + 22번 미노출                       |
| 11  | 백업 검증         | 7~9개월차           | **Phase 2(4개월차) + 월 1회 정례**            | 검증 안 한 백업은 백업이 아니다                       |
| 12  | 자원 산정         | requests 상한만 제시  | **1노드 장애 기준 검증 + PriorityClass 축소 운전** | 정량 검증                                    |
| 13  | JVM 설정        | 언급 없음            | **§3 전체**                              | JVM 특성 반영 요구                             |
| 14  | 잔여 크레딧        | 소진율 99.9%        | **98% + Phase 4 재원으로 계획**              | 변동비 초과 방어                                |
| 15  | 요금 근거         | 계산기 전제           | **계산기 실제 단가표 추출**                      | 검증 가능성                                   |


## 유지된 판단

원본에서 그대로 유지한 결정과 그 이유다. 재검토 결과 여전히 최선이다.


| 판단                                          | 유지 근거                                                       |
| ------------------------------------------- | ----------------------------------------------------------- |
| K3s (kubeadm·RKE2·k0s 대신)                   | 9개월의 병목은 도구 학습이 아니라 운영 완주. Control Plane 오버헤드 절감 = JVM heap |
| 3노드 통합형 (CP/Worker 분리 대신)                   | 같은 예산에서 저사양 6노드보다 충분한 사양 3노드가 안정적                           |
| embedded etcd ×3 (외부 DB 대신)                 | 외부 DB는 추가 HA 비용과 장애 지점                                      |
| Flannel VXLAN (Cilium 대신)                   | eBPF 이득이 이 규모에서 계측되지 않음. CNI는 교체 비용이 가장 큰 컴포넌트              |
| kube-proxy (eBPF replacement 대신)            | 위와 동일                                                       |
| 가비아 External LB (MetalLB·kube-vip 대신)       | 외부 진입점과 헬스 체크를 관리형으로                                        |
| Traefik (ingress-nginx 대신)                  | ingress-nginx는 2026년 3월 은퇴, 저장소 read-only, 보안 패치 없음         |
| cert-manager (Traefik 내장 ACME 대신)           | 3 replica에서 인증서 중앙 관리                                       |
| local-path (Longhorn 대신)                    | Longhorn 권장 4 vCPU에 미달. 복제는 PostgreSQL 계층에서 해결              |
| Alloy → Grafana Cloud (내부 스택 대신)            | 관측 백엔드를 관측 대상과 같은 클러스터에 두면 장애 시 진단 수단을 동시에 잃음               |
| PSA + NetworkPolicy (Kyverno·Gatekeeper 대신) | 정책 엔진은 정책을 운영할 인력이 있을 때 가치 있음 (운영 2명)                       |
| Kubeflow 미도입                                | GPU·ML 파이프라인 요구 없음. LLM API 호출은 Deployment·Job으로 충분         |
| Istio 미도입                                   | 서비스 3개 미만에서 서비스 메시는 순수 오버헤드                                 |


---

## 이 설계의 원칙

1. **예산이 늘면 결론을 다시 계산한다.** 원본의 Flux·SOPS·PG 단일 Primary는 12GB 제약의
 산물이었다. 제약이 사라졌는데 결론이 같으면 재검토를 하지 않은 것이다.
2. **검증하지 않은 것은 완료가 아니다.** 백업·HA·무중단 배포는 모두 “실제로 깨뜨려 보고
 복구한 기록”이 있을 때만 DoD를 통과한다.
3. **자원 한계를 숫자로 안다.** 1노드 장애 시 12.0Gi vs requests 13.0Gi라는 계산과, 그때
 무엇이 먼저 밀려나는지(PriorityClass)를 미리 정한다.
4. **기술을 많이 설치하지 않는다.** 3노드 HA에 필요한 것만 유지하고, 고급 기능은 실제 요구가
 생기거나 별도 재원으로 검증한 뒤 도입한다. (원본의 결론이며 여기서도 유지한다.)
5. **끝을 설계에 넣는다.** 9개월 뒤 크레딧이 만료되는 것은 리스크가 아니라 알려진 조건이다.
 Phase 4의 이관 리허설과 잔여 크레딧 집행 계획은 처음부터 설계의 일부다.

