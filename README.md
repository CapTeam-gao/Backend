# CapTeam Backend

CapTeam Backend는 캡스톤 프로젝트 팀 구성과 운영 흐름을 지원하는 Spring Boot 기반 API 서버입니다.

학생 설문 데이터, 관리자 팀 추천 요청, AI 서버 매칭 결과, 확정 팀 관리, 공지, 일지, 채팅 기능을 하나의 백엔드 도메인으로 연결합니다. 프론트엔드는 화면과 사용자 흐름을 담당하고, 백엔드는 인증, 권한, 데이터 저장, AI 서버 연동, 팀 생성 상태 관리, 실시간 통신을 담당합니다.

## 프로젝트 목적

캡스톤 팀 구성은 단순히 인원 수만 맞추는 문제가 아닙니다. 학생의 희망 직군, 기술 스택, 구현 경험, 성향 점수, 선호 팀원, 팀장 희망 여부를 함께 고려해야 합니다.

CapTeam Backend는 이 데이터를 안정적으로 저장하고, 관리자 요청에 따라 AI 서버에 팀 매칭을 요청한 뒤, 추천안 검토와 승인 과정을 거쳐 실제 팀 데이터로 확정하는 흐름을 제공합니다.

팀 확정 이후에는 프로젝트 기획서, 공지, 캡스톤 일지, 팀 채팅 기능이 같은 팀 데이터를 기준으로 동작하도록 API를 제공합니다.

## 핵심 사용자

| 사용자 | 백엔드에서 지원하는 기능 |
| --- | --- |
| 학생 | 로그인, 설문 제출, 내 팀 조회, 선호 팀원 저장, 프로젝트 기획서 작성, 공지 조회, 일지 작성, 팀 채팅 |
| 관리자 | 학생 조회, 설문 완료 여부 확인, AI 팀 추천 요청, 추천안 조회/수정/승인, 확정 팀 관리, 공지 관리, 일지 관리, 채팅 관리 |

## 주요 흐름

```text
로그인
 ├─ JWT accessToken 발급
 ├─ refreshToken HttpOnly Cookie 발급
 └─ 사용자 권한 및 설문 완료 여부 반환

학생 설문
 ├─ 희망 직군/기술 스택/구현 경험 저장
 ├─ 성격 성향/개발 성향 점수 저장
 ├─ 선호 팀원 및 팀장 희망 여부 저장
 └─ 설문 제출 후 학생 분석 생성 시도

관리자 팀 추천
 ├─ 학년별 매칭 job 생성
 ├─ AI 서버에 학생 payload 전달
 ├─ job_id 기준 상태 조회/취소
 ├─ AI 추천안 저장
 ├─ 추천안 멤버 교체
 └─ 추천안 승인 시 실제 Team/TeamUser 생성

팀 운영
 ├─ 확정 팀 목록/상세 조회
 ├─ 프로젝트 기획서 작성
 ├─ 공지 CRUD
 ├─ 캡스톤 일지 작성/조회
 └─ WebSocket 기반 팀 채팅
```

## 주요 기능

### 인증과 권한

- Spring Security 기반 인증 필터를 사용합니다.
- JWT accessToken을 통해 API 요청 사용자를 식별합니다.
- refreshToken은 HttpOnly Cookie 기반 재발급 API를 지원합니다.
- 학생과 관리자의 API 접근 권한을 분리합니다.
- 비밀번호 변경 API를 제공합니다.

### 학생 설문과 분석

- 학생 설문 응답을 `User` 엔티티에 저장합니다.
- 희망 직군, 기술 스택, 구현 경험, 선호 팀원, 팀장 희망 여부를 관리합니다.
- 성격 성향과 개발 성향 점수를 각각 별도 임베디드 값으로 저장합니다.
- 설문 제출 후 AI 분석 응답이 존재하면 `UserAnalysis`를 생성하거나 갱신합니다.
- AI 서버 장애가 발생해도 설문 저장 자체는 실패시키지 않도록 분리했습니다.

### AI 팀 추천과 작업 관리

- 관리자는 학년별로 팀 추천 작업을 요청할 수 있습니다.
- 팀 매칭 작업은 즉시 완료 응답을 기다리지 않고 job 형태로 등록됩니다.
- `jobId` 기준으로 진행 상태 조회와 취소 요청을 처리합니다.
- AI 서버 호출 시 `X-Matching-Job-Id` 헤더를 전달해 백엔드와 AI 서버의 작업 단위를 맞춥니다.
- 추천안 재생성 요청에서는 `regenerationPrompt`를 AI 서버 payload에 포함합니다.
- AI 추천 결과의 팀 강점과 약점은 추천안에 저장하고, 추천 승인 시 실제 팀으로 복사합니다.

### 추천안 검토와 팀 확정

- 관리자는 추천안 목록과 상세 정보를 조회할 수 있습니다.
- 추천안에는 팀원, 추천 역할, 추천 팀장, 배정 이유가 포함됩니다.
- 추천안 승인 전 학생 2명을 선택해 팀원을 교체할 수 있습니다.
- 추천안 승인 시 실제 `Team`, `TeamUser`, 채팅방, 기본 채널을 생성합니다.
- 확정 팀 상세 응답에는 기획서 팀명(`projectTeamName`), 서비스명, 주요 기능, AI 강점/약점이 포함됩니다.

### 관리자 학생 관리

- 전체 학생 목록을 조회하고 이름, 학번, 희망 직군, 학년, 설문 완료 여부로 필터링할 수 있습니다.
- 학생 상세 조회에서 설문 입력 정보, 기술 스택, 구현 경험, 선호 팀원, 분석 결과, 성향 점수를 확인할 수 있습니다.
- 설문을 제출하지 않은 학생도 목록에서 확인할 수 있도록 응답을 구성합니다.

### 공지, 일지, 채팅

- 관리자 공지 작성, 수정, 삭제, 조회 API를 제공합니다.
- 학생은 공지 목록과 상세 내용을 조회할 수 있습니다.
- 캡스톤 일지 작성과 관리자 조회 흐름을 제공합니다.
- WebSocket/STOMP 기반 채팅 API를 구성하고, 팀별 채팅방과 채널을 관리합니다.
- 채팅 파일은 비공개 S3 버킷에 저장하고 채널 권한 확인 후 Presigned URL로 제공합니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security, JWT |
| Database | MySQL, Spring Data JPA |
| Realtime | WebSocket, STOMP |
| Storage/Infra | Amazon S3, EC2 IAM Role |
| API Docs | springdoc-openapi |
| Build | Gradle |
| Test | JUnit 5, Mockito, Spring Boot Test |
| Deploy | Docker Compose, Nginx, EC2 |

## 폴더 구조

```text
src/main/java/com/capteam/gaobackend
 ├─ ai              # AI 서버 호출 클라이언트와 AI payload/응답 처리
 ├─ config          # Security, JWT, WebSocket, 파일, executor 설정
 ├─ controller      # 학생/공통 API 컨트롤러
 │   └─ admin       # 관리자 API 컨트롤러
 ├─ dto             # API 요청/응답 DTO
 ├─ entity          # JPA 엔티티
 ├─ enums           # 권한, 학년, 역할, 상태 enum
 ├─ exception       # 공통 예외와 에러 처리
 ├─ repository      # Spring Data JPA repository
 ├─ security        # Security UserDetails 및 핸들러
 ├─ service         # 비즈니스 로직
 │   └─ admin       # 관리자 도메인 서비스
 └─ util            # 검증, 점수 계산 등 유틸
```

## 주요 API

### 인증

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/auth/login` | 로그인 및 토큰 발급 |
| POST | `/api/auth/reissue` | HttpOnly Cookie refreshToken 기반 토큰 재발급 |
| POST | `/api/auth/refresh` | 요청 body refreshToken 기반 토큰 재발급 |
| GET | `/api/auth/me` | 현재 인증 상태 조회 |
| PUT | `/api/auth/password` | 비밀번호 변경 |

### 학생

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/user/survey` | 내 설문 조회 |
| POST | `/api/user/survey` | 내 설문 제출 |
| GET | `/api/user/me/profile` | 내 프로필 조회 |
| PUT | `/api/user/me/profile` | 내 프로필 수정 |
| GET | `/api/teams/my-team` | 내 팀 상세 조회 |
| GET | `/api/teams/my-team/summary` | 내 팀 요약 조회 |
| GET | `/api/teams/project` | 프로젝트 기획서 조회 |
| POST | `/api/teams/project` | 프로젝트 기획서 생성 |
| PUT | `/api/teams/project` | 프로젝트 기획서 수정 |

### 관리자 팀 추천/팀 관리

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/admin/team-recommendations/matching/run` | AI 팀 추천 job 생성 |
| GET | `/api/admin/team-recommendations/matching/jobs/{jobId}` | 매칭 job 상태 조회 |
| DELETE | `/api/admin/team-recommendations/matching/jobs/{jobId}` | 매칭 job 취소 |
| GET | `/api/admin/team-recommendations` | 추천안 목록 조회 |
| GET | `/api/admin/team-recommendations/{recommendationId}` | 추천안 상세 조회 |
| POST | `/api/admin/team-recommendations/{recommendationId}/accept` | 추천안 승인 |
| POST | `/api/admin/team-recommendations/swap` | 추천안 팀원 교체 |
| POST | `/api/admin/team-recommendations/accept-all/{grade}` | 학년별 추천안 일괄 승인 |
| GET | `/api/admin/teams` | 확정 팀 목록 조회 |
| GET | `/api/admin/teams/{teamId}` | 확정 팀 상세 조회 |

### 관리자 운영

| Method | Path | 설명 |
| --- | --- | --- |
| GET | `/api/admin/students` | 학생 목록 조회 |
| GET | `/api/admin/students/{userId}` | 학생 상세 조회 |
| GET | `/api/admin/dashboard` | 관리자 대시보드 조회 |
| GET/POST/PUT/DELETE | `/api/admin/notices` | 관리자 공지 관리 |
| GET | `/api/admin/journals` | 관리자 일지 조회 |
| GET | `/api/admin/chat` | 관리자 채팅 관리 |

## AI 서버 연동

백엔드는 FastAPI 기반 AI 서버와 HTTP로 통신합니다.

기본 AI 서버 주소는 아래 설정으로 관리합니다.

```properties
AI_SERVER_BASE_URL=http://localhost:8000
```

팀 매칭 요청 시 백엔드는 학생 설문 데이터를 AI payload로 변환해 `/matching/run`으로 전달합니다.

재생성 프롬프트가 있는 경우 payload는 다음 형태가 됩니다.

```json
{
  "students": [],
  "regeneration_prompt": "백엔드 역할을 강화해서 다시 추천해줘"
}
```

AI 작업 취소를 위해 백엔드는 동일한 `jobId`를 `X-Matching-Job-Id` 헤더와 취소 API에 사용합니다.

## 환경 변수

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/gao
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=****
JWT_SECRET=****
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=1209600000
JWT_REFRESH_COOKIE_SECURE=false
JWT_REFRESH_COOKIE_SAME_SITE=Lax
WEBSOCKET_ALLOWED_ORIGINS=https://frontend.example.com
AI_SERVER_BASE_URL=http://localhost:8000
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=your-private-chat-file-bucket
AWS_S3_KEY_PREFIX=chat
AWS_S3_PRESIGNED_URL_DURATION=PT10M
```

민감 정보는 `.env` 또는 배포 환경 변수로 관리하고, 저장소에 포함하지 않습니다.
운영 EC2에는 S3 객체 업로드와 조회 권한을 가진 IAM Role을 연결하며, Access Key는 환경 변수로 저장하지 않습니다.
S3 버킷은 퍼블릭 액세스를 차단하고, EC2 IAM Role에는 해당 버킷의 `s3:PutObject`, `s3:GetObject` 권한만 부여합니다.
백엔드는 `/chat-files/{channelId}/{fileName}` 경로를 저장하고, 같은 채널에 접근 가능한 로그인 사용자에게만 짧게 유효한 Presigned URL을 생성합니다.

## EC2 + S3 운영 배포

운영 배포에 필요한 IAM 정책, S3 CORS, Nginx 설정과 실행 순서는 [`deploy/aws/README.md`](deploy/aws/README.md)에 정리되어 있습니다.

핵심 원칙은 다음과 같습니다.

- EC2 IAM Role의 임시 자격 증명을 사용하고 Access Key를 저장하지 않습니다.
- S3 버킷은 비공개로 유지하고 `chat/*` 객체에만 `GetObject`, `PutObject`를 허용합니다.
- MySQL과 Spring Boot 포트는 EC2 외부에 직접 공개하지 않고 Nginx의 80/443만 공개합니다.
- `/actuator/health`를 Docker와 EC2 상태 확인에 사용합니다.

## 실행 방법

```bash
./gradlew bootRun
```

또는 Docker 환경에서 실행합니다.

```bash
docker compose up --build
```

## 테스트

```bash
./gradlew test
```

특정 테스트만 실행할 때는 다음처럼 실행합니다.

```bash
./gradlew test --tests com.capteam.gaobackend.service.admin.AdminTeamServiceTest
```

## 개발 상태

| 기능 | 상태 |
| --- | --- |
| JWT 로그인/재발급 | 구현 |
| 학생 설문 저장 | 구현 |
| 설문 후 학생 분석 생성 | 구현 |
| 관리자 학생 조회 | 구현 |
| AI 팀 추천 job 생성/조회/취소 | 구현 |
| 추천안 조회/교체/승인 | 구현 |
| 확정 팀 관리 | 구현 |
| 프로젝트 기획서 API | 구현 |
| 공지 CRUD | 구현 |
| 캡스톤 일지 | 구현 및 확장 중 |
| WebSocket 채팅 | 구현 및 확장 중 |

## Git Workflow

```bash
git checkout main
git pull origin main
git checkout -b feature/작업명

git add .
git commit -m "작업 내용"
git push origin feature/작업명
```

Pull Request 생성 후 코드 리뷰를 거쳐 main 브랜치에 병합합니다.
