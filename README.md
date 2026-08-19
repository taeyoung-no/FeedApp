# Feed App

TDD, Docker, AWS 배포, CI/CD, 부하 테스트 등 포함 실무에서 사용한다고 알려진 기술 스택을 학습하기 위한 Spring Boot, React CRUD 프로젝트입니다. [링크](https://feed.taeyoung-no.com)

## 목차
1. [기능](#기능)
2. [기술 스택](#기술-스택)
3. [개발 노트](#개발-노트)
4. [로컬 실행](#로컬-실행)

## 기능
- 회원 기능
- 글 작성, 목록 조회, 상세 조회, 수정, 삭제 (이미지 포함, S3 Presigned URL)
- 댓글 작성, 목록 조회, 수정, 삭제

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

## 개발 노트
- [N+1 문제가 그렇게 치명적인가? 증거 있음?](https://taeyoung-no.github.io/2026/08/08/n+1.html)
- [ECS: 업데이트 시 deployment 실패한 이유](https://taeyoung-no.github.io/2026/08/09/ecs.html)

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
# 터미널 1
./server/gradlew -p server bootRun

# 터미널 2
npm run dev --prefix client
```

### 단위, 통합 테스트
```bash
./server/gradlew -p server test
```

### 부하 테스트
```bash
# 시드 데이터
docker compose -f server/docker-compose.yml exec -T mysql mysql -u admin -pqwer1234 feedapp < server/loadtest-seed.sql

k6 run loadtest/feed-list.js
```
