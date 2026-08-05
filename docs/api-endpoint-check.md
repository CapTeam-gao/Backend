# API 엔드포인트 점검 메모

기준: 사용자가 전달한 API 명세서와 현재 백엔드 컨트롤러 매핑을 대조한 결과입니다.

주의: 이 문서는 개발 진행률 표기가 아니라, 프론트/백엔드가 맞춰야 할 엔드포인트 기준 문서입니다.

## 최종발표 피드백 재학습 요약

- 선호 팀원 입력은 `{학번 이름}` 텍스트 입력보다 학생 검색/선택 방식이 맞다.
- 설문 미응답 학생 FCM 알림은 제외한다. 로그인하면 설문 미완료 학생은 설문 화면으로 이동하고, FCM 토큰도 로그인 이후 등록되므로 실효성이 낮다.
- 캡스톤 일지 마감 알림은 유지보수 후보로 남긴다.
- 직접 팀 구성은 AI 추천 결과를 대체/보완하는 관리자 기능으로 본다.
- 팀 재생성 버전 비교, 진행률 표시, 중간 결과 조회는 이후 확장 기능으로 분리한다.
- 직군 쏠림, 중복 배정, 미배정 학생은 백엔드 검증으로 막아야 한다.

## 명세서에서 바로 수정해야 하는 경로

| 기능 | 명세서 경로 | 현재 백엔드 기준 경로 | 메모 |
| --- | --- | --- | --- |
| 비밀번호 변경 | `PATCH /api/auth/password` | `PUT /api/auth/password` | 실제 컨트롤러는 `@PutMapping` |
| 로그인 상태 확인 | 없음 | `GET /api/auth/me` | 프론트의 "로그인 상태 확인 중" 흐름에서 사용 가능 |
| 토큰 재발급 쿠키 방식 | 없음 | `POST /api/auth/reissue` | HttpOnly cookie refresh token 사용 |
| 로그아웃 | 없음 | `POST /api/auth/logout` | refresh token 쿠키 삭제 |
| 내 팀 조회 | `GET /api/teams/me` | `GET /api/teams/my-team` | 실제 팀 상세 조회 |
| 내 팀 요약 조회 | 없음 | `GET /api/teams/my-team/summary` | 대시보드/요약 UI에 사용 가능 |
| 팀 상세 조회 | 없음 | `GET /api/teams/{teamId}` | 로그인 학생이 소속된 팀 상세 |
| 팀 프로젝트 조회 | 없음 | `GET /api/teams/project` | 팀 기획서 조회 |
| 팀 프로젝트 저장 | 없음 | `POST /api/teams/project`, `PUT /api/teams/project` | 생성/수정 모두 upsert |
| 파일 다운로드 | `GET /chat-files/{fileName}` | `GET /chat-files/{channelId}/{fileName}` | 실제 정적 다운로드 경로는 channelId 포함 |
| 파일 다운로드 URL | 없음 | `GET /api/chat/channels/{channelId}/files/{fileName}/download-url` | 백엔드 API 방식 다운로드 URL |
| AI 팀 생성 요청 | `POST /api/admin/team-recommendations` | `POST /api/admin/team-recommendations` 또는 `POST /api/admin/team-recommendations/matching/run` | 동기/비동기 추천 요청이 나뉨 |
| 추천팀 승인 | `POST /api/admin/team-recommendations/{recommendationId}/approve` | `POST /api/admin/team-recommendations/{recommendationId}/accept` | 실제 경로는 `accept` |
| 수동 팀 생성 | `POST /api/admin/teams/manual` | `POST /api/admin/teams/manual`, `POST /api/admin/team-recommendations/manual` | 프론트 호환 경로는 즉시 실제 팀 생성, 추천안 도메인 경로는 PENDING 추천안 저장 |
| 팀원 수정/이동 | `PATCH /api/admin/teams/{teamId}/members` | `PATCH /api/admin/teams/{teamId}/members` | 실제 존재 |
| 관리자 채팅방 공지 작성 | `POST /api/admin/chat/rooms/{roomId}/notice` | 추가 필요 | 현재 컨트롤러에 없음 |
| 관리자 공지 목록/상세 | `GET /api/admin/notices`, `GET /api/admin/notices/{noticeId}` | `GET /api/notices`, `GET /api/notices/{noticeId}` 또는 관리자 전용 API 추가 필요 | 현재 admin controller에는 생성/수정/삭제만 있음 |

## 명세서에 추가할 엔드포인트

### 학생 검색

| 기능 | HTTP | API Path | 토큰 | 요청 |
| --- | --- | --- | --- | --- |
| 선호 팀원 검색 | `GET` | `/api/students/search` | 필요 | `keyword` |
| 선호 팀원 검색 보조 경로 | `GET` | `/api/user/students/search` | 필요 | `keyword` |
| 관리자 직접 구성 학생 검색 | `GET` | `/api/admin/students/search` | 필요 | `grade`, `keyword` |

학생용 검색 조건:

- 로그인한 학생과 같은 학년만 검색한다.
- 본인은 검색 결과에서 제외한다.
- 이름 또는 학번 일부로 검색한다.
- 빈 검색어는 빈 배열을 반환한다.
- 결과는 최대 6명만 반환한다.

관리자용 검색 조건:

- `grade`는 필수다. 예: `GRADE_2`, `GRADE_3`
- `keyword` 하나로 이름, 학번, 직군을 검색한다.
- 직군 키워드는 한글/영문 별칭을 일부 허용한다. 예: `프론트`, `frontend`, `백엔드`, `backend`, `앱`, `디자인`, `ai`, `devops`, `보안`
- 빈 검색어는 빈 배열을 반환한다.
- 결과는 최대 10명만 반환한다.

### 직접 팀 구성

| 기능 | HTTP | API Path | 토큰 | 요청 |
| --- | --- | --- | --- | --- |
| 관리자 직접 구성 추천안 생성 | `POST` | `/api/admin/team-recommendations/manual` | 필요 | `grade`, `teams[]` |
| 관리자 직접 구성 팀 생성 호환 경로 | `POST` | `/api/admin/teams/manual` | 필요 | `grade`, `teams[]` |

요청 예시:

```json
{
  "grade": "GRADE_2",
  "teams": [
    {
      "teamNumber": 1,
      "members": [
        {
          "userId": "stu2301",
          "role": "FRONTEND",
          "leader": true
        },
        {
          "userId": "stu2302",
          "role": "BACKEND",
          "leader": false
        }
      ]
    }
  ]
}
```

프론트 직접 구성 화면이 보내는 요청도 함께 허용한다:

```json
{
  "grade": "GRADE_2",
  "teams": [
    {
      "teamName": "1팀",
      "memberUserIds": ["stu2301", "stu2302"],
      "leaderUserId": "stu2301"
    }
  ]
}
```

백엔드 검증:

- 한 팀 최대 5명
- 팀당 팀장 최대 1명
- 같은 학생 중복 배정 금지
- 해당 학년 학생만 배정 가능
- 이미 확정 팀에 들어간 학생 배정 금지
- 미배정 학생이 남아 있어도 프론트 확인 모달 이후 진행 가능

경로별 처리 차이:

- `POST /api/admin/team-recommendations/manual`: 직접 구성 결과를 PENDING 추천안으로 저장한다. 이후 추천안 승인 API로 실제 팀을 만든다.
- `POST /api/admin/teams/manual`: 현재 프론트 직접 구성 화면 호환 경로다. 직접 구성 결과를 저장한 뒤 즉시 해당 학년 추천안을 전체 승인해서 실제 팀과 기본 채팅방까지 생성한다.

## 실제 백엔드 엔드포인트 전체 목록

### 인증

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 로그인 | `POST` | `/api/auth/login` |
| 비밀번호 변경 | `PUT` | `/api/auth/password` |
| 로그인 상태 확인 | `GET` | `/api/auth/me` |
| 토큰 재발급 | `POST` | `/api/auth/refresh` |
| 쿠키 기반 토큰 재발급 | `POST` | `/api/auth/reissue` |
| 로그아웃 | `POST` | `/api/auth/logout` |

### 내 정보/설문/학생 검색

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 내 프로필 조회 | `GET` | `/api/user/me/profile` |
| 내 프로필 수정 | `PUT` | `/api/user/me/profile` |
| 내 설문 조회 | `GET` | `/api/user/survey` |
| 내 설문 저장 | `POST` | `/api/user/survey` |
| 선호 팀원 검색 | `GET` | `/api/user/students/search` |
| 헤더바 정보 | `GET` | `/api/user/header` |
| 선호 팀원 검색 호환 경로 | `GET` | `/api/students/search` |

### 공지

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 공지 목록 조회 | `GET` | `/api/notices` |
| 공지 상세 조회 및 읽음 처리 | `GET` | `/api/notices/{noticeId}` |
| 공지 읽음 처리 | `POST` | `/api/notices/{noticeId}/read` |

### 팀

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 선호 팀원 조회 | `GET` | `/api/teams/preference` |
| 선호 팀원 저장 | `POST` | `/api/teams/preference` |
| 내 팀 상세 조회 | `GET` | `/api/teams/my-team` |
| 내 팀 요약 조회 | `GET` | `/api/teams/my-team/summary` |
| 특정 팀 상세 조회 | `GET` | `/api/teams/{teamId}` |
| 팀 프로젝트 조회 | `GET` | `/api/teams/project` |
| 팀 프로젝트 생성/수정 | `POST` | `/api/teams/project` |
| 팀 프로젝트 생성/수정 | `PUT` | `/api/teams/project` |

### 채팅

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 내 채팅방 조회 | `GET` | `/api/chat/rooms/my` |
| 내 채널 요약 목록 조회 | `GET` | `/api/chat/rooms/my/channel-summaries` |
| 채팅방 상세 조회 | `GET` | `/api/chat/rooms/{roomId}` |
| 채널 생성 | `POST` | `/api/chat/rooms/{roomId}/channels` |
| 채널 수정 | `PATCH` | `/api/chat/channels/{channelId}` |
| 채널 삭제 | `DELETE` | `/api/chat/channels/{channelId}` |
| 파일 업로드 | `POST` | `/api/chat/channels/{channelId}/files` |
| 메시지 목록 조회 | `GET` | `/api/chat/channels/{channelId}/messages` |
| 메시지 작성 | `POST` | `/api/chat/channels/{channelId}/messages` |
| 메시지 수정 | `PATCH` | `/api/chat/messages/{messageId}` |
| 메시지 삭제 | `DELETE` | `/api/chat/messages/{messageId}` |
| 읽음 처리 | `POST` | `/api/chat/channels/{channelId}/read` |
| 접속 현황 조회 | `GET` | `/api/chat/channels/{channelId}/presence` |
| 파일 다운로드 | `GET` | `/chat-files/{channelId}/{fileName}` |
| 파일 다운로드 URL 조회 | `GET` | `/api/chat/channels/{channelId}/files/{fileName}/download-url` |

### 캡스톤 일지

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 일지 작성 | `POST` | `/api/journals` |
| 일지 목록 조회 | `GET` | `/api/journals` |
| 오늘 일지 조회 | `GET` | `/api/journals/today` |
| 완성 일지 조회 | `GET` | `/api/journals/{journalId}` |
| 일지 수정 | `PATCH` | `/api/journals/{journalId}` |

### 대시보드

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 관리자 대시보드 | `GET` | `/api/admin/dashboard` |
| 학생 대시보드 | `GET` | `/api/user/dashboard` |

### 관리자 학생 관리

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 학생 목록 조회 | `GET` | `/api/admin/students` |
| 직접 구성용 학생 검색 | `GET` | `/api/admin/students/search` |
| 학생 상세 조회 | `GET` | `/api/admin/students/{userId}` |

### 관리자 팀 추천/직접 구성

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 비동기 AI 팀 생성 요청 | `POST` | `/api/admin/team-recommendations/matching/run` |
| 비동기 AI 팀 생성 작업 조회 | `GET` | `/api/admin/team-recommendations/matching/jobs/{jobId}` |
| 비동기 AI 팀 생성 작업 취소 | `DELETE` | `/api/admin/team-recommendations/matching/jobs/{jobId}` |
| AI 팀 추천 생성 | `POST` | `/api/admin/team-recommendations` |
| 직접 구성 추천안 생성 | `POST` | `/api/admin/team-recommendations/manual` |
| 추천 팀 목록 조회 | `GET` | `/api/admin/team-recommendations` |
| 추천 팀 상세 조회 | `GET` | `/api/admin/team-recommendations/{recommendationId}` |
| 추천 팀 단건 승인 | `POST` | `/api/admin/team-recommendations/{recommendationId}/accept` |
| 학년별 추천 팀 조회 | `GET` | `/api/admin/team-recommendations/grade/{grade}` |
| 추천 팀원 교체 | `POST` | `/api/admin/team-recommendations/swap` |
| 학년별 추천안 전체 승인 | `POST` | `/api/admin/team-recommendations/accept-all/{grade}` |

### 관리자 팀 관리

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 전체 팀 조회 | `GET` | `/api/admin/teams` |
| 팀 상세 조회 | `GET` | `/api/admin/teams/{teamId}` |
| 팀원 역할/팀장/이동 수정 | `PATCH` | `/api/admin/teams/{teamId}/members` |
| 직접 구성 팀 생성 호환 경로 | `POST` | `/api/admin/teams/manual` |

### 관리자 공지

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 공지 작성 | `POST` | `/api/admin/notices` |
| 공지 수정 | `PUT` | `/api/admin/notices/{noticeId}` |
| 공지 삭제 | `DELETE` | `/api/admin/notices/{noticeId}` |

### 관리자 채팅

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 전체 채팅방 목록 조회 | `GET` | `/api/admin/chat/rooms` |
| 전체 안 읽은 메시지 요약 | `GET` | `/api/admin/chat/unread-summary` |
| 채팅방 생성 | `POST` | `/api/admin/chat/rooms` |
| 채팅방 상세 조회 | `GET` | `/api/admin/chat/rooms/{roomId}` |
| 채팅방 채널 요약 조회 | `GET` | `/api/admin/chat/rooms/{roomId}/channel-summaries` |
| 채팅방 삭제 | `DELETE` | `/api/admin/chat/rooms/{roomId}` |
| 팀별 채팅방 조회 | `GET` | `/api/admin/chat/teams/{teamId}/room` |
| 채널 메시지 조회 | `GET` | `/api/admin/chat/channels/{channelId}/messages` |
| 관리자 읽음 처리 | `POST` | `/api/admin/chat/channels/{channelId}/read` |

### 관리자 캡스톤 일지

| 기능 | HTTP | API Path |
| --- | --- | --- |
| 일지 전체 목록 조회 | `GET` | `/api/admin/journals` |
| 일지 상세 조회 | `GET` | `/api/admin/journals/{journalId}` |

### 관리자 AI

| 기능 | HTTP | API Path |
| --- | --- | --- |
| AI 팀 요약 조회 | `GET` | `/api/admin/ai/teams/summary` |
| 학생 AI 분석 일괄 실행 | `POST` | `/api/admin/ai/analysis/run` |
| 학생 분석 후 AI 매칭 실행 | `POST` | `/api/admin/ai/matching/run` |

## 명세서에 있으나 현재 백엔드에 없는 API

이 항목들은 프론트에서 실제로 필요하면 백엔드에 추가 구현이 필요합니다.

| 기능 | 요청 경로 |
| --- | --- |
| 내 AI 분석 요청 | `POST /api/me/analysis` |
| 내 AI 분석 결과 조회 | `GET /api/me/analysis` |
| 팀 역할 분포 확인 | `GET /api/teams/role` |
| 관리자 학생 추가 | `POST /api/admin/students` |
| 추천팀 거절 | `POST /api/admin/team-recommendations/{recommendationId}/reject` |
| 관리자 팀 추가 | `POST /api/admin/teams` |
| 관리자 팀 수정 | `PUT /api/admin/teams/{teamId}` |
| 관리자 팀 삭제 | `DELETE /api/admin/teams/{teamId}` |
| 관리자 공지 목록 조회 | `GET /api/admin/notices` |
| 관리자 공지 상세 조회 | `GET /api/admin/notices/{noticeId}` |
| 공지 읽음 현황 조회 | `GET /api/admin/notices/{noticeId}/reads` |
| 관리자 채팅방 공지 작성 | `POST /api/admin/chat/rooms/{roomId}/notice` |
| 일지 피드백 | `POST /api/admin/journals/{journalId}/feedback` |
