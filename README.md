# 🚌 Bus Tracker (실시간 버스 도착 정보 & 다기기 동기화 서비스)

[![Live Demo](https://img.shields.io/badge/Live%20Demo-seoul--bus.duckdns.org-brightgreen?style=flat-square&logo=googlechrome)](https://seoul-bus.duckdns.org)
[![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue.js-3.5-4FC08D?style=flat-square&logo=vuedotjs)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6.x-646CFF?style=flat-square&logo=vite)](https://vitejs.dev/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)
[![GCP](https://img.shields.io/badge/GCP-Compute%20Engine-4285F4?style=flat-square&logo=googlecloud)](https://cloud.google.com/)
[![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?style=flat-square&logo=githubactions)](https://github.com/features/actions)

> **서울 및 경기 지역의 실시간 버스 도착 정보를 빠르게 확인하고, 게스트 모드 및 Google OAuth 다기기 동기화로 즐겨찾기를 관리할 수 있는 반응형 웹 서비스입니다.**  
> 🔗 **서비스 링크:** [https://seoul-bus.duckdns.org](https://seoul-bus.duckdns.org)

---

## 📌 목차
- [1. 프로젝트 개요](#-1-프로젝트-개요)
- [2. 개발 환경 & AI 페어 프로그래밍 (AI-Assisted Engineering)](#-2-개발-환경--ai-페어-프로그래밍-ai-assisted-engineering)
- [3. 시스템 아키텍처](#-3-시스템-아키텍처)
- [4. 핵심 기능](#-4-핵심-기능)
- [5. 기술 스택](#-5-기술-스택)
- [6. 핵심 트러블슈팅 & 성능 최적화](#-6-핵심-트러블슈팅--성능-최적화)
- [7. CI/CD 배포 파이프라인](#-7-cicd-배포-파이프라인)

---

## 🌟 1. 프로젝트 개요

대중교통 출퇴근길에 사용자가 가장 자주 타는 버스의 실시간 위치와 도착 예정 시간을 즉시 확인할 수 있도록 개발되었습니다.  
특히 **로그인 없이도 즉시 사용 가능한 기기 로컬 모드**와, **Google 계정 연동을 통해 PC, 태블릿, 모바일 등 어떤 기기에서든 동일한 즐겨찾기가 유지되는 클라우드 동기화 기능**을 제공합니다.

* **운영 환경:** Google Cloud Platform (GCP Compute Engine e2-micro, Ubuntu)
* **도메인 & 보안:** DuckDNS (DDNS), Let's Encrypt SSL/TLS (HTTPS 자동 리다이렉트 및 갱신)
* **배포 방식:** GitHub Actions 기반 Docker Hub 빌드/푸시 및 GCP SSH 무중단 배포

---

## 🤖 2. 개발 환경 & AI 페어 프로그래밍 (AI-Assisted Engineering)

본 프로젝트는 차세대 AI 페어 프로그래밍 워크플로우를 적극 활용하여 기획, 풀스택 개발, 클라우드 인프라 구축, 배포 트러블슈팅 전 과정을 기민하게 완성했습니다.

| 구분 | 사용 도구 및 환경 | 활용 내용 |
|---|---|---|
| **IDE** | **Google Antigravity IDE** | AI 에이전트 기반 차세대 통합 개발 환경으로 코드 작성, 인프라 진단 및 터미널 자동화 수행 |
| **AI Agent** | **Antigravity AI Agent** | 자율적 코드 분석, 파일 편집, 빌드/배포 상태 모니터링, SSH 및 Docker 원격 제어 페어 프로그래밍 |
| **AI Models** | **Claude Sonnet (3.5 / 3.7 Sonnet)**<br>**Gemini Flash (2.0 / 3.8 Flash)** | • **Sonnet:** 도메인 로직 설계, JWT 인증 시스템, Spring Boot 아키텍처 및 복잡한 Nginx SSL 설정<br>• **Flash:** 빠른 실시간 로그 분석, CI/CD 배포 모니터링, 데이터 파싱 및 반응형 UI 레이아웃 최적화 |

> 💡 **협업 방식:** 기능 명세 수립 → AI 에이전트와의 아키텍처 설계 검토 → Docker 및 Nginx 인프라 코드 생성 → GitHub Actions 연동 및 GCP 실서버 배포 검증까지 전 단계를 인간-AI 협력 구조로 진행했습니다.

---

## 🏗️ 3. 시스템 아키텍처

```mermaid
flowchart TB
    subgraph ClientLayer [Client Devices]
        Mobile["📱 Mobile Web (PWA/Browser)"]
        Desktop["💻 Desktop Browser"]
    end

    subgraph DNS [DNS & Network]
        DuckDNS["🌐 DuckDNS (seoul-bus.duckdns.org)"]
    end

    subgraph GCP [GCP Compute Engine : e2-micro]
        subgraph HostOS [Host OS : Ubuntu Linux]
            Cron["⏰ Host Cron (Certbot Auto Renewal)"]
            HostData["💾 Host Storage (~/app/data)\nPersistent DB Volume"]
            LetsEncryptCerts["🔒 /etc/letsencrypt (SSL Certificates)"]
        end

        subgraph DockerNet [Docker Bridge Network : bus-tracker-net]
            subgraph FrontendContainer [Frontend Container (Nginx:alpine)]
                Nginx80["Port 80 (HTTP)\nACME Challenge & 301 Redirect"]
                Nginx443["Port 443 (HTTPS/SSL)\nReverse Proxy & SPA Static Server"]
                VueApp["Vue 3 + Vite Single Page Application"]
            end

            subgraph BackendContainer [Backend Container (JRE 21 Alpine)]
                SpringBoot["Spring Boot 3.3.4\n(JVM Heap: 128MB~256MB 제한)"]
                AuthModule["JWT & Google OAuth Resolver"]
                BusDataModule["Bus Arrival Cache & Provider"]
                EmbeddedDB["H2 DB (MySQL Mode)\n./data/busdb.mv.db"]
            end
        end
    end

    subgraph ExternalServices [External Public APIs & Auth]
        GoogleAuth["🔑 Google Identity Services (OAuth 2.0)"]
        SeoulBusAPI["🚌 서울시 버스도착정보 Open API"]
        PublicBusAPI["🚍 공공데이터포털 버스도착정보 API"]
        KakaoMap["🗺️ Kakao Map API"]
    end

    %% Client Access
    ClientLayer -->|HTTPS Request| DuckDNS
    DuckDNS -->|34.10.106.205| Nginx443
    ClientLayer -->|HTTP Request| Nginx80
    Nginx80 -->|301 Redirect to HTTPS| Nginx443

    %% Inside Docker
    Nginx443 -->|Static Files / SPA Routing| VueApp
    Nginx443 -->|/api/* Proxy Pass| SpringBoot

    %% Storage Mount
    LetsEncryptCerts -.->|Mounted :ro| Nginx443
    HostData -.->|Mounted :rw| EmbeddedDB
    Cron -.->|Renew Certs| LetsEncryptCerts

    %% External Connections
    VueApp -->|Render Map| KakaoMap
    VueApp -->|Client Sign-In| GoogleAuth
    SpringBoot -->|Verify Token| GoogleAuth
    SpringBoot -->|Fetch Real-time Arrival Info| SeoulBusAPI
    SpringBoot -->|Fetch Arrival Info Fallback| PublicBusAPI
```

---

## ⚡ 4. 핵심 기능

### 1) 실시간 버스 도착 정보 조회 & 자동 새로고침
* 정류장 검색(명칭 및 ARS-ID) 및 경유 노선별 실시간 도착 예정 시간, 남은 정류장 수 표기
* 버스 상태(운행중, 곧 도착, 막차, 차고지 대기 등) 뱃지 시각화
* 15초 단위 실시간 자동 새로고침 및 수동 새로고침 인터랙션

### 2) 다기기 클라우드 동기화 (Google OAuth 2.0 & Guest 모드)
* **무가입 게스트 모드:** 브라우저 고유 UUID를 기반으로 로그인 없이도 즉시 즐겨찾기 저장 가능
* **Google 간편 로그인:** Google Identity Services(GSI) SDK를 연동하여 원클릭 로그인 지원
* **데이터 무손실 마이그레이션:** 게스트 상태에서 등록한 즐겨찾기가 있는 상태에서 구글 로그인 시, 기존 데이터를 계정 DB로 자동 병합

### 3) 반응형 모바일 친화적 UI / UX
* 데스크톱 및 스마트폰 화면 비율에 최적화된 유연한 카드 그리드 및 네비게이션
* 카카오 지도 SDK를 통한 정류장 위치 시각화

---

## 🛠️ 5. 기술 스택

### Frontend
| 기술 | 버전/설명 |
|---|---|
| **Vue.js 3** | Composition API (`<script setup>`) 기반 선언적 컴포넌트 개발 |
| **Vite** | 초고속 HMR 및 최적화된 번들링 |
| **Pinia** | 인증 상태(`authStore`) 및 즐겨찾기(`bookmarkStore`) 전역 상태 관리 |
| **TailwindCSS** | 유틸리티 퍼스트 반응형 스타일링 및 다크/글래스모피즘 룩앤필 |
| **Axios** | 인터셉터를 통한 JWT 토큰 및 기기 식별 헤더 자동 주입 |
| **Lucide Vue Next** | 모던 벡터 아이콘 셋 |

### Backend
| 기술 | 버전/설명 |
|---|---|
| **Java 21** | LTS 버전의 모던 Java 문법 및 성능 최적화 적용 |
| **Spring Boot 3.3.4** | RESTful API 서버 구축 |
| **Spring Data JPA / Hibernate** | 객체 중심 ORM 및 DDL Auto 스키마 제어 |
| **H2 Database (MySQL Mode)** | MySQL 호환 임베디드 파일 DB + 영구 볼륨 연동 |
| **JWT (jjwt 0.12.5)** | Stateless 인증 토큰 발급 및 검증 |
| **XML / JSON 파서** | 공공데이터 포털의 다양한 XML/JSON 응답 정규화 처리 |

### DevOps & Infrastructure
| 기술 | 설명 |
|---|---|
| **GCP Compute Engine** | Ubuntu 22.04 LTS (e2-micro 인스턴스) |
| **Docker & Docker Compose** | 프론트엔드/백엔드 멀티 컨테이너화 및 격리 배포 |
| **Nginx (Alpine)** | 정적 자원 서빙, SSL 종단, 백엔드 리버스 프록시 |
| **Let's Encrypt / Certbot** | 무료 정식 SSL/TLS 인증서 발급 및 자동 갱신 크론 구성 |
| **DuckDNS** | 무료 DDNS를 통한 고정 도메인(`seoul-bus.duckdns.org`) 바인딩 |
| **GitHub Actions** | Push 시 자동 Gradle 빌드, Docker 이미지 패키징, GCP SSH 원격 배포 자동화 |

---

## 💡 6. 핵심 트러블슈팅 & 성능 최적화

포트폴리오 관점에서 프로젝트 수행 중 직면했던 기술적 문제들과 그 해결 과정입니다:

### 1) GCP e2-micro (1GB RAM) 저사양 환경 OOM 방지
* **문제:** GCP 프리티어 인스턴스의 1GB 메모리 한계로 인해 Spring Boot 기동 시 Out of Memory(OOM)로 인스턴스가 멈추는 현상 발생 가능.
* **해결:**
  * Dockerfile에서 JVM 실행 옵션을 `-Xms128m -Xmx256m`으로 제한하여 힙 메모리 사용량을 명시적으로 통제.
  * CI/CD 파이프라인에서 무거운 빌드 작업(Gradle build, Docker build)은 **GitHub Actions Runner(Ubuntu 최신)**에서 수행하고, GCP 서버에서는 가벼운 `docker compose pull && up`만 수행하도록 책임 분리.

### 2) 무상태(Stateless) 컨테이너 재배포 시 DB 유실 문제 해결
* **문제:** 새 기능 배포 시 기존 백엔드 컨테이너가 삭제(`docker rm`)되면서 컨테이너 내부에 저장되던 즐겨찾기 DB 파일(`busdb.mv.db`)이 초기화되는 문제 발견.
* **해결:**
  * `deploy.yml`의 Docker Compose 구성에 호스트 볼륨 마운트(`volumes: - ./data:/app/data`)를 적용.
  * 컨테이너를 수십 번 재생성하더라도 GCP VM 호스트 디스크의 영구 스토리지에 데이터가 유지되도록 보장.

### 3) 무중단 자동 갱신을 포함한 HTTPS (SSL/TLS) 아키텍처 구축
* **문제:** 구글 OAuth(GSI)는 보안 정책상 HTTPS 도메인이 필수이며, 도커 컨테이너 환경에서 90일마다 만료되는 Let's Encrypt 인증서를 갱신해야 하는 과제.
* **해결:**
  * CI/CD 초기 배포 시 호스트의 Certbot 독립 실행형(`--standalone`)을 통해 인증서를 선발급받고, `/etc/letsencrypt` 디렉터리를 Nginx 컨테이너에 읽기 전용(`:ro`)으로 마운트.
  * Nginx에 포트 80(HTTP) → 443(HTTPS) 301 자동 리다이렉트 및 `/.well-known/acme-challenge/` 경로 처리 구성.
  * 호스트의 `crontab`에 인증서 자동 갱신 스크립트를 등록하여 무중단 자동 갱신 체계 완성.

### 4) 게스트-회원 간의 데이터 무손실 동기화 (Migration) 로직
* **문제:** 로그인 전 편하게 쓰던 사용자가 구글 로그인을 했을 때 기존에 기기에 저장했던 버스 목록이 덮어씌워지거나 사라지는 UX 저하 우려.
* **해결:**
  * 기기 UUID 기반의 게스트 데이터베이스를 별도로 식별하고, 구글 로그인 완료 시점에 백엔드 서비스 계층에서 중복 검사(`existsByUserIdAnd...`)를 거쳐 게스트 북마크를 대상 구글 계정으로 트랜잭션 내에서 안전하게 마이그레이션.

---

## 🚀 7. CI/CD 배포 파이프라인

코드 변경 후 `main` 브랜치에 푸시하면 GitHub Actions가 감지하여 배포 전 과정을 자동으로 수행합니다:

```
[ Git Push (main) ]
         ↓
[ GitHub Actions Runner ]
  1. JDK 21 환경 세팅 & Gradle 빌드 (JAR 산출물 생성)
  2. Docker Hub 로그인
  3. 백엔드 Docker 이미지 빌드 및 Docker Hub 푸시 (:latest)
  4. 프론트엔드 Docker 이미지 빌드 및 Docker Hub 푸시 (:latest)
         ↓
[ SSH Action to GCP Compute Engine ]
  5. SSL 인증서 발급 확인 (최초 미발급 시 자동 발급)
  6. 최신 Docker Compose 설정 동적 생성 (볼륨 및 포트 443 마운트)
  7. 최신 도커 이미지 Pull & 무중단 컨테이너 기동 (docker compose up -d)
  8. 미사용 도커 이미지 정리 (docker image prune -f)
```

---

## 👨‍💻 Author

* **Developer:** Dongho Lee ([@DonghoLeee](https://github.com/DonghoLeee))
* **Repository:** [https://github.com/DonghoLeee/bus-tracker](https://github.com/DonghoLeee/bus-tracker)
* **Service:** [https://seoul-bus.duckdns.org](https://seoul-bus.duckdns.org)
