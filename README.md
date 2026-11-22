# Feed App

TDD, Docker, AWS 배포, CI/CD, 부하 테스트 등 포함 실무에서 사용한다고 알려진 기술 스택을 학습하기 위한 Spring Boot, React CRUD 프로젝트입니다. [링크](https://feed.taeyoung-no.com)

## 목차
1. [개발 노트](#개발-노트)
2. [기술 스택](#기술-스택)
3. [기능](#기능)
4. [로컬 실행](#로컬-실행)

## 개발 노트
[N+1 문제가 그렇게 치명적인가? 증거 있음?](https://taeyoung-no.github.io/2026/08/08/n+1.html)
- 게시글 목록 조회 시 게시글마다 이미지를 따로 조회하는 N+1 문제를 JOIN FETCH로 해결, p95 1480ms에서 529ms로 감소

[좋아요 기능 성능 개선 및 동시성 문제 해결](https://taeyoung-no.github.io/2026/09/03/race-condition.html)
- 게시글마다 좋아요 수를 `COUNT(*)`로 세는 문제를 게시글에 좋아요 수를 저장해서 해결, p95 약 1300ms에서 약 550ms로 감소
- 1000명이 동시에 좋아요를 누르면 좋아요 수가 100~103만 증가하는 동시성 문제를 원자적 `UPDATE`로 해결

[인덱스 스캔 50만 vs. 풀 테이블 스캔 100만](https://taeyoung-no.github.io/2026/09/16/index-vs-full-table.html)
- offset 페이지네이션에서 동시 요청 시 중간 페이지가 마지막 페이지보다 느린 원인을 버퍼 풀 동시 접근 병목으로 분석, VU 10에서 중간 페이지 p95 3.620s, 마지막 페이지 1.326s

[인기글 조회 시 Cache Stampede 해결과 응답 지연 개선](https://taeyoung-no.github.io/2026/11/22/feed-cache.html)
- 인기글 조회가 초당 100건을 처리하지 못하는 문제를 Redis 캐시로 해결, p95 3.49s에서 337.82ms로 감소
- 캐시 만료 시 요청이 몰려 데이터베이스를 538번 조회하는 Cache Stampede를 singleflight로 해결, 26번으로 감소, p95 110.57ms
- singleflight에서 캐시가 채워질 때까지 요청이 기다리는 문제를 SWR로 해결, p95 110.57ms에서 5.39ms로 감소

[JWT 안전하게 사용하기](https://taeyoung-no.github.io/2026/09/04/jwt.html)
- 탈취된 JWT를 즉시 폐기할 수 없는 문제를 짧은 액세스 토큰 유효기간과 저장소에 보관한 리프레시 토큰으로 보완
- 리프레시 토큰 탈취를 알아차리기 어려운 문제를 리프레시 토큰 로테이션으로 감지

[ECS: 업데이트 시 deployment 실패한 이유](https://taeyoung-no.github.io/2026/08/09/ecs.html)
- 서버가 뜨는 약 120초 동안 헬스 체크에 실패해 배포가 항상 실패하는 문제를 grace period 300초로 해결

## 기술 스택
핵심이라고 생각하는 것만 정리했습니다.

| 영역 | 기술 |
|------|------|
| 프론트엔드 | React, TypeScript |
| 백엔드 | Spring Boot, Java |
| DB | MySQL |
| 캐시, 세션 | Redis |
| 파일 스토리지 | AWS S3 |
| 단위, 통합 테스트 | JUnit, Testcontainers |
| 부하 테스트 | k6 |
| 인프라 | Docker |
| 배포 | AWS ECR, ECS, S3, CloudFront |
| CI/CD | GitHub Actions |

## 기능
- 회원 기능
- 글 작성, 목록 조회, 상세 조회, 수정, 삭제 (이미지 포함, S3 Presigned URL)
- 인기글 조회 (최근 1일 좋아요 순, Redis 캐시)
- 댓글 작성, 목록 조회, 수정, 삭제

## 로컬 실행
### 요구사항
- Java 21
- Node.js 22+
- Docker, Docker Compose
- npm
- k6 (부하 테스트 시)

### 의존성
```bash
npm install --prefix client
```

### 인프라
```bash
docker compose -f server/docker-compose.yml up -d
```

### 개발 서버
```bash
# 백엔드
./server/gradlew -p server bootRun
```

```bash
# 프론트엔드
npm run dev --prefix client
```
