# CapTeam 유지보수 백엔드 구현 정리

이 문서는 최종발표 피드백을 바탕으로 CapTeam 프로젝트에서 백엔드가 구현해야 할 유지보수 기능을 정리한 것이다.

중요: 이번 범위에서는 **설문 미응답 학생 FCM 알림 기능은 제외**한다. 알림 기능은 일지 마감 알림, 공지 알림, 채팅 알림 중심으로 정리한다.

## 0. 현재 프로젝트 구조 요약

CapTeam은 크게 3개 파트로 나뉜다.

- `Frontend`: React/Vite 기반 사용자 화면
- `Backend`: Spring Boot 기반 API 서버
- `AI`: FastAPI/LangChain 기반 학생 분석 및 팀 매칭 서버

백엔드는 다음 역할을 담당한다.

- JWT 인증과 권한 분리
- 학생 설문 저장
- 학생 분석 및 AI 서버 연동
- 관리자 팀 추천 요청과 추천 결과 저장
- 팀 추천안 검토, 수정, 승인
- 확정 팀 관리
- 공지, 일지, 채팅 기능
- WebSocket/STOMP 기반 실시간 채팅
- MySQL 데이터 저장

알림 기능을 추가하더라도 MySQL은 그대로 사용한다.

```text
MySQL
= 학생, 설문, 일지, 공지, 채팅, 팀 데이터 저장

Firebase FCM
= 브라우저/앱으로 푸시 알림을 보내는 외부 발송 통로
```

Firebase를 DB로 사용하는 것이 아니라, Firebase Cloud Messaging만 알림 발송 인프라로 붙인다.

## 1. 유지보수 전체 기능 범위

설문 미응답 FCM 알림을 제외하면 백엔드가 구현해야 할 기능은 다음과 같다.

1. 선호 팀원 검색 API 및 선호 팀원 저장 검증
2. 캡스톤 일지 마감 FCM 알림
3. FCM 토큰 등록/관리
4. 공지 등록 FCM 알림
5. 채팅 메시지 FCM 알림
6. 팀 추천 결과 버전 저장
7. 팀 재생성 전후 diff 계산
8. 직접 팀 구성 저장 및 검증
9. AI 팀 생성 진행률/작업 상태 확장
10. LLM 결과 검증 및 처리 로그 저장
11. 팀 채팅 소통 기능 보완
12. 로그인 후 캡스톤/해커톤 선택 분기 검토

## 2. 알림에서 Firebase를 사용하는 이유

브라우저나 앱에 백그라운드 푸시 알림을 보내려면 백엔드가 학생 기기에 직접 알림을 보낼 수 없다. 브라우저와 OS가 알림 권한, 백그라운드 수신, 기기별 토큰을 관리하기 때문이다.

따라서 구조는 다음처럼 된다.

```text
Spring Boot Backend
-> Firebase Cloud Messaging
-> 학생 브라우저/앱
```

FCM이 해주는 일은 다음과 같다.

- 브라우저/기기별 푸시 토큰 발급
- 백그라운드 알림 전달
- Chrome/Android 등 환경별 푸시 라우팅
- 만료 토큰, 실패 응답 반환
- 다수 사용자 대상 발송 지원

백엔드는 Firebase Admin SDK를 사용해서 FCM에 발송 요청만 보낸다.

## 3. 알림 권한 승인 여부

알림을 보내기 위해 필요한 권한은 두 종류로 나누어 이해해야 한다.

### 백엔드 권한

백엔드 자체는 사용자에게 별도 권한 승인을 받을 필요가 없다. 대신 Firebase 프로젝트에서 발급한 서비스 계정 키 또는 인증 정보를 서버 환경변수로 설정해야 한다.

### 사용자 알림 권한

학생 브라우저/앱에서 푸시 알림을 받으려면 사용자가 알림 권한을 허용해야 한다.

흐름은 다음과 같다.

```text
학생 로그인
-> 프론트에서 알림 권한 요청
-> 학생이 허용
-> 프론트가 FCM 토큰 발급
-> 프론트가 백엔드에 FCM 토큰 등록
-> 백엔드가 필요한 시점에 해당 토큰으로 푸시 발송
```

사용자가 알림 권한을 거부하면 백엔드는 토큰을 받을 수 없거나 유효한 토큰이 없으므로 푸시를 보낼 수 없다. 이 경우 서비스 내부 토스트나 공지 목록 같은 대체 UI로 보완해야 한다.

## 4. 공통 알림 기반 설계

일지 마감 알림, 공지 알림, 채팅 알림은 공통 기반을 공유하도록 만든다.

### 신규 엔티티: FcmToken

```text
FcmToken
- id: Long
- user: User
- token: String
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

설계 기준:

- `token`은 unique로 둔다.
- 한 사용자가 여러 기기/브라우저를 사용할 수 있으므로 user 1명에 token 여러 개를 허용한다.
- 같은 token이 다시 등록되면 새 row를 만들지 말고 updatedAt만 갱신한다.
- 로그아웃 또는 토큰 만료 응답을 받으면 삭제하거나 비활성화한다.

### 신규 엔티티: NotificationLog

```text
NotificationLog
- id: Long
- user: User
- notificationType: NotificationType
- targetId: Long
- status: NotificationStatus
- sentAt: LocalDateTime
- errorMessage: String
```

`targetId` 예시:

- 일지 알림: journalId
- 공지 알림: noticeId
- 채팅 알림: messageId 또는 channelId

### 신규 enum: NotificationType

```text
JOURNAL_DEADLINE
NOTICE_CREATED
CHAT_MESSAGE
```

설문 미응답 알림은 이번 구현 범위에서 제외하므로 `SURVEY_REMINDER`는 만들지 않거나, 추후 확장을 위해 문서에만 보류한다.

### 신규 enum: NotificationStatus

```text
SENT
FAILED
```

추후 필요하면 다음 상태를 확장할 수 있다.

```text
SKIPPED
INVALID_TOKEN
```

### 신규 Repository

```text
FcmTokenRepository
NotificationLogRepository
```

필요 메서드 예시:

```java
Optional<FcmToken> findByToken(String token);
List<FcmToken> findByUserId(Long userId);
List<FcmToken> findByUserIdIn(Collection<Long> userIds);
void deleteByToken(String token);

boolean existsByUserIdAndNotificationTypeAndTargetId(
    Long userId,
    NotificationType notificationType,
    Long targetId
);
```

### 신규 Service

```text
FcmTokenService
FirebaseMessagingService
NotificationService
```

역할:

- `FcmTokenService`: 토큰 등록, 갱신, 삭제
- `FirebaseMessagingService`: Firebase Admin SDK로 실제 발송
- `NotificationService`: 알림 대상 조회, payload 생성, 발송 로그 저장, 실패 처리

### 신규 API

```text
POST /api/user/fcm-token
DELETE /api/user/fcm-token
```

토큰 등록 요청:

```json
{
  "fcmToken": "fcm-token-value"
}
```

토큰 삭제 요청은 body 없이 현재 로그인 사용자에게 연결된 FCM 토큰을 삭제한다.

응답은 기존 공통 envelope를 따른다.

```json
{
  "success": true,
  "message": "FCM 토큰이 등록되었습니다.",
  "data": null
}
```

## 5. Firebase Admin SDK 설정

백엔드에는 Firebase Admin SDK 의존성을 추가한다.

`Backend/build.gradle` 예시:

```gradle
implementation 'com.google.firebase:firebase-admin:9.3.0'
```

현재 프로젝트는 `firebase-admin` 9.3.0을 사용한다.

설정값은 `application.properties` 또는 환경변수로 둔다.

```properties
firebase.enabled=${FIREBASE_ENABLED:false}
firebase.project-id=${FIREBASE_PROJECT_ID:}
firebase.credentials-json=${FIREBASE_CREDENTIALS_JSON:}
firebase.credentials-file=${FIREBASE_CREDENTIALS_FILE:}
```

운영 서버에서는 서비스 계정 JSON 파일을 repo에 커밋하지 않는다.

권장 방식:

- 로컬: `/path/to/firebase-service-account.json` 경로를 환경변수로 설정
- 운영: EC2 또는 배포 환경에 secret 파일/환경변수로 주입
- Git에는 절대 서비스 계정 키를 올리지 않음

초기화 클래스 예시:

```text
FirebaseConfig
- FirebaseApp이 이미 초기화되어 있으면 재초기화하지 않음
- credentials path가 없으면 알림 발송 기능만 비활성화하거나 명확한 에러 로그 출력
```

## 6. 캡스톤 일지 마감 알림

### 목적

캡스톤 일지를 제출하지 않은 학생에게 마감 임박 알림을 자동 발송한다.

설문 미응답 알림과 달리 관리자 수동 버튼은 만들지 않는다. 문서 기준으로 일지 알림은 **마감 30분 전 자동 발송만** 진행한다.

### 백엔드 구성

```text
JournalDeadlineNotificationScheduler
JournalDeadlineNotificationService
NotificationService
FcmTokenRepository
NotificationLogRepository
```

### 동작 흐름

```text
스케줄러가 주기적으로 실행
-> 마감 30분 전 대상 일지 조회
-> 제출 대상자 조회
-> 제출 완료자 제외
-> 미제출 학생 목록 생성
-> 이미 같은 journalId로 JOURNAL_DEADLINE 알림을 보낸 학생 제외
-> 학생들의 FCM 토큰 조회
-> FCM 발송
-> NotificationLog 저장
```

### 중복 발송 방지

`NotificationLog`를 기준으로 다음 조건이 이미 있으면 재발송하지 않는다.

```text
userId + notificationType(JOURNAL_DEADLINE) + targetId(journalId)
```

### 예외 처리

- 일지를 이미 제출한 학생은 발송 대상에서 제외
- 마감 시간이 지난 일지는 발송하지 않음
- 토큰이 없는 학생은 발송 생략
- FCM 발송 실패는 일지 기능 자체 실패로 전파하지 않음
- 만료/유효하지 않은 토큰 응답은 삭제 또는 비활성화 처리

### FCM payload 예시

```json
{
  "notification": {
    "title": "캡스톤 일지 마감 임박",
    "body": "아직 제출하지 않은 캡스톤 일지가 있습니다."
  },
  "data": {
    "type": "JOURNAL_DEADLINE",
    "journalId": "10",
    "path": "/user/log"
  }
}
```

## 7. 공지 등록 FCM 알림

### 목적

관리자가 공지를 등록하면 전체 학생 또는 대상 학생에게 FCM 푸시 알림을 보낸다.

### 기존 예상 흐름

```text
AdminNoticeController
-> AdminNoticeService
-> NoticeRepository.save()
```

### 추가 흐름

```text
공지 저장 성공
-> NotificationService.sendNoticeCreated(...)
-> 대상 학생의 FCM 토큰 조회
-> FirebaseMessagingService로 발송
-> NotificationLog 저장
```

### 구현 위치

`AdminNoticeService`에서 공지 저장 트랜잭션이 성공한 뒤 알림을 발송한다.

알림 발송 실패 때문에 공지 생성이 실패하면 안 된다. 따라서 다음 중 하나를 선택한다.

1. 공지 저장 후 별도 try/catch로 알림 발송
2. `@TransactionalEventListener(phase = AFTER_COMMIT)`로 커밋 후 알림 발송

권장 방식은 2번이다. 공지 저장이 롤백되었는데 알림만 나가는 문제를 막을 수 있다.

### FCM payload 예시

```json
{
  "notification": {
    "title": "새 공지사항",
    "body": "새 공지가 등록되었습니다."
  },
  "data": {
    "type": "NOTICE_CREATED",
    "noticeId": "15",
    "path": "/user/notice/15"
  }
}
```

### 주의 사항

- 공지 저장 실패 시 알림 발송 금지
- 알림 실패가 공지 생성 실패로 이어지면 안 됨
- 관리자에게 발송 결과 통계가 필요하면 `NotificationLogRepository` 기반 조회 API를 추가
- `TEAM_RESULT` 공지와 일반 공지를 구분해서 메시지 문구를 다르게 할 수 있음

## 8. 채팅 메시지 FCM 알림

### 목적

팀 채팅 메시지가 도착했을 때 수신자에게 알림을 보낸다.

프론트가 웹사이트를 보고 있는 포그라운드 상태에서는 푸시 대신 WebSocket 이벤트를 받아 토스트로 보여주는 것이 좋다. 백그라운드 또는 미접속 상태에서는 FCM 푸시를 보낸다.

### 기존 예상 흐름

```text
ChatService
-> 메시지 저장
-> WebSocket/STOMP 전송
```

### 추가 흐름

```text
메시지 저장
-> 수신 대상자 조회
-> 발신자 제외
-> 접속/포그라운드 상태 확인
-> FCM 대상자에게 푸시 발송
-> NotificationLog 저장
```

### 수신 대상

- 같은 채팅방/채널에 속한 사용자
- 메시지 작성자는 제외
- 권한이 없는 사용자는 제외

### 접속 상태 판단

현재 프로젝트에는 `ChatPresenceService`가 있으므로 이를 우선 활용한다.

다만 "웹사이트를 보고 있는 포그라운드 상태"와 "WebSocket만 연결된 상태"는 다를 수 있다. 초기 구현에서는 다음 정도로 단순화할 수 있다.

```text
온라인 사용자: WebSocket 이벤트만 전송, 프론트에서 토스트 처리
오프라인 사용자: FCM 푸시 발송
```

더 정확히 하려면 프론트가 visibility 상태를 백엔드에 알려주는 API 또는 WebSocket 이벤트가 필요하다.

### FCM payload 예시

```json
{
  "notification": {
    "title": "새 팀 채팅 메시지",
    "body": "새 메시지가 도착했습니다."
  },
  "data": {
    "type": "CHAT_MESSAGE",
    "roomId": "3",
    "channelId": "7",
    "messageId": "100",
    "path": "/user/chat"
  }
}
```

### 주의 사항

- 메시지 본문 전체를 푸시에 담지 않는 것이 안전함
- 민감한 내용이 있을 수 있으므로 body는 간단하게 유지
- FCM 실패가 채팅 메시지 저장 실패로 이어지면 안 됨
- 다중 토큰 사용자에게는 모든 유효 토큰으로 발송

## 9. 선호 팀원 검색 및 저장 검증

### 목적

학생이 선호 팀원을 직접 문자열로 입력하지 않고, 실제 학생 데이터를 검색해서 선택하게 한다.

백엔드는 학생 검색 API와 설문 저장 시 검증을 담당한다.

### 신규 API

```text
GET /api/students/search?keyword={keyword}
```

관리자 전용이 아니라 학생도 호출 가능해야 한다.

응답 `data` 예시:

```json
[
  {
    "userId": 12,
    "name": "김민수",
    "grade": "GRADE_3",
    "classNumber": 1,
    "number": 5
  }
]
```

민감정보는 포함하지 않는다.

포함 금지 예시:

- 전화번호
- 이메일
- 상세 설문 응답
- 분석 결과
- 개인 성향 점수

### Repository 검색 조건

`UserRepository`에 이름/학번 검색 메서드를 추가한다.

검색 기준:

- 이름 일부
- 학번 일부
- 학생 계정만
- 필요하면 같은 학년만
- 최대 6건 정도 제한

### 설문 저장 검증

`POST /api/user/survey` 처리 시 `preferredTeammates`를 서버에서 재검증한다.

검증 항목:

- 존재하는 학생 `userId`인지
- 본인 `userId`가 아닌지
- 중복이 없는지
- 최대 3명을 넘지 않는지
- 학생 계정인지

프론트에서 이미 막더라도 백엔드 검증은 반드시 필요하다.

### 저장 방식

문서 기준으로 설문 저장 필드는 유지한다.

```text
preferredTeammates: string[]
```

다만 실제 값은 이름 문자열이 아니라 학생 `userId` 문자열 배열로 통일한다.

예시:

```json
{
  "preferredTeammates": ["12", "18", "24"]
}
```

기존 데이터가 이름/학번 문자열로 저장되어 있다면 마이그레이션 여부를 검토한다.

## 10. 팀 추천 결과 버전 저장

### 목적

팀 재생성 시 기존 결과를 바로 덮어쓰지 않고, 이전 결과와 새 결과를 모두 보관한다. 관리자가 새 결과를 확인하고 적용하기 전까지 기존 결과는 유지한다.

### 현재 구조 추정

현재 백엔드에는 다음 엔티티가 존재한다.

```text
TeamRecommendation
TeamRecommendationMember
TeamRecommendationReason
MatchingJob
```

추천 결과 저장은 이미 구현되어 있으므로, 버전 단위를 추가하는 방식이 자연스럽다.

### 신규 엔티티: TeamRecommendationVersion

```text
TeamRecommendationVersion
- id: Long
- grade: Grade
- versionNumber: Integer
- status: RecommendationVersionStatus
- sourceType: RecommendationVersionSourceType
- regenerationPrompt: String
- createdAt: LocalDateTime
- appliedAt: LocalDateTime
```

### 신규 enum: RecommendationVersionStatus

```text
DRAFT
APPLIED
ARCHIVED
```

### 신규 enum: RecommendationVersionSourceType

```text
INITIAL
REGENERATED
MANUAL
```

### 기존 엔티티 변경

`TeamRecommendation`에 version 관계를 추가한다.

```text
TeamRecommendation
- version: TeamRecommendationVersion
- teamNumber
- strength
- weakness
```

### API 초안

```text
GET /api/admin/team-recommendations/versions
GET /api/admin/team-recommendations/versions/{versionId}
POST /api/admin/team-recommendations/versions/{versionId}/regenerate
POST /api/admin/team-recommendations/versions/{versionId}/apply
POST /api/admin/team-recommendations/versions/{versionId}/rollback
```

rollback은 필수는 아니지만, 이전 버전 복구를 지원하려면 추가한다.

### 핵심 규칙

- 최초 AI 생성 결과는 `INITIAL` 버전으로 저장
- 재생성 결과는 `REGENERATED` 새 버전으로 저장
- 직접 구성 결과는 `MANUAL` 새 버전으로 저장
- 새 버전 생성만으로 확정 팀을 바꾸지 않음
- `apply` 호출 시에만 실제 Team/TeamUser 또는 적용 버전 상태를 변경
- 한 학년에서 `APPLIED` 버전은 하나만 유지

## 11. 팀 재생성 전후 diff 계산

### 목적

관리자가 기존 팀 구성과 재생성 결과를 한눈에 비교할 수 있도록 변경 내역을 계산한다.

### API 초안

```text
GET /api/admin/team-recommendations/versions/{beforeVersionId}/diff/{afterVersionId}
```

### diff 계산 방식

이전 버전과 새 버전의 멤버를 `userId` 기준으로 map으로 만든다.

```text
beforeMap: userId -> teamNumber, role, leader
afterMap: userId -> teamNumber, role, leader
```

비교 항목:

- 이동 학생: teamNumber 변경
- 역할 변경 학생: recommendedRole 변경
- 팀장 변경 학생: leader 변경
- 누락 학생: 이전에는 있었는데 새 버전에 없음
- 신규 학생: 새 버전에만 있음
- 중복 배정 학생: 새 버전에서 한 userId가 여러 번 등장

### 응답 예시

```json
{
  "beforeVersionId": 1,
  "afterVersionId": 2,
  "movedStudents": [
    {
      "userId": 12,
      "name": "김민수",
      "fromTeamNumber": 1,
      "toTeamNumber": 3,
      "fromRole": "BACKEND",
      "toRole": "BACKEND"
    }
  ],
  "roleChangedStudents": [],
  "leaderChangedStudents": [],
  "missingStudents": [],
  "newStudents": [],
  "duplicatedStudents": [],
  "teamChanges": [
    {
      "teamNumber": 1,
      "memberCountBefore": 5,
      "memberCountAfter": 4,
      "roleSummaryBefore": {
        "FRONTEND": 2,
        "BACKEND": 2,
        "AI": 1
      },
      "roleSummaryAfter": {
        "FRONTEND": 1,
        "BACKEND": 2,
        "AI": 1
      }
    }
  ]
}
```

## 12. 직접 팀 구성

### 목적

관리자가 AI 자동 배정이 아니라 직접 학생을 팀에 배치할 수 있게 한다.

문서 기준 팀 구성 방식은 2가지로 나눈다.

```text
AI 자동 배정
직접 팀 구성
```

`학생 희망 팀 제출 후 AI 보정` 방식은 제거하기로 결정되어 있으므로 구현하지 않는다.

### API 초안

```text
POST /api/admin/team-recommendations/manual
```

요청 예시:

```json
{
  "grade": "GRADE_3",
  "teams": [
    {
      "teamNumber": 1,
      "members": [
        {
          "userId": 1,
          "role": "FRONTEND",
          "leader": true
        },
        {
          "userId": 2,
          "role": "BACKEND",
          "leader": false
        }
      ]
    }
  ]
}
```

응답 예시:

```json
{
  "versionId": 3,
  "warnings": [
    {
      "type": "UNASSIGNED_STUDENTS",
      "message": "미배정 학생 2명이 있습니다.",
      "userIds": [10, 11]
    }
  ]
}
```

### 검증 항목

저장 차단 오류:

- 존재하지 않는 userId
- 학생 계정이 아닌 userId
- 선택한 학년과 다른 학생
- 한 학생이 여러 팀에 중복 배정
- 팀당 최대 5명 초과
- 한 팀에 팀장 2명 이상

경고로 처리 가능한 항목:

- 미배정 학생 존재
- 특정 역할 쏠림
- 팀별 인원 불균형
- 팀장 없는 팀

저장 차단 오류는 `400 Bad Request`로 반환한다. 경고는 저장은 허용하되 응답에 `warnings`로 내려준다.

## 13. AI 팀 생성 진행률 및 작업 상태 확장

### 목적

팀 생성이 오래 걸릴 때 빈 로딩 화면만 보여주지 않고 진행 상태를 확인할 수 있게 한다.

현재 프로젝트에는 이미 다음 구조가 있다.

```text
MatchingJob
MatchingJobService
MatchingJobWorker
MatchingJobStateService
AdminAiTeamMatchingController
```

따라서 신규 구조를 완전히 만들기보다 기존 matching job을 확장한다.

### 기존 API

```text
POST /api/admin/team-recommendations/matching/run
GET /api/admin/team-recommendations/matching/jobs/{jobId}
DELETE /api/admin/team-recommendations/matching/jobs/{jobId}
```

### MatchingJob 추가 필드 후보

```text
progressPercent: Integer
currentStep: String
completedTeamCount: Integer
totalTeamCount: Integer
partialResultJson: String 또는 JSON column
errorMessage: String
startedAt: LocalDateTime
finishedAt: LocalDateTime
retryCount: Integer
durationMs: Long
```

이미 비슷한 필드가 있으면 중복 추가하지 말고 기존 필드를 재사용한다.

### 상태 흐름

```text
QUEUED
-> RUNNING
-> COMPLETING
-> SUCCEEDED
```

실패/취소:

```text
FAILED
CANCELLED
```

### 단계 예시

```text
학생 데이터 수집 중
AI 서버 요청 준비 중
팀 매칭 생성 중
결과 검증 중
추천안 저장 중
완료
```

처음에는 AI 서버가 중간 결과를 반환하지 않더라도 백엔드 단계 진행률만 보여줄 수 있다. 이후 AI 서버가 배치 단위 결과를 지원하면 `partialResultJson`에 완료된 팀부터 저장한다.

## 14. LLM 결과 검증 및 로깅

### 목적

AI 결과를 그대로 믿고 저장하지 않고, 백엔드에서 확실한 규칙을 검증한다. 학생 누락, 중복 배정, 존재하지 않는 학생 같은 문제는 코드로 잡아야 한다.

### 신규 서비스 후보

```text
AiTeamRecommendationValidator
AiMatchingMetricsService
```

### 검증 항목

- AI 응답 JSON 파싱 가능 여부
- 없는 userId 포함 여부
- 요청 대상 학생 중 누락된 학생 여부
- 한 학생이 여러 팀에 중복 배정되었는지
- 팀당 최대 인원 초과 여부
- 역할 분포가 너무 한쪽으로 몰렸는지
- 팀장 후보가 없는지
- 같은 팀 내 선호/회피 조건 충돌 여부

### 실패 처리

검증 실패 시 다음 중 하나를 선택한다.

1. job을 `FAILED`로 종료하고 관리자에게 오류 사유 표시
2. 가능한 경우 fallback 로직으로 재조정
3. 일부 실패 구간만 재시도

초기 구현은 1번이 가장 단순하고 안전하다.

### 로깅/측정 항목

`MatchingJob` 또는 별도 metric 엔티티에 다음 값을 기록한다.

```text
durationMs
retryCount
aiRequestSize
aiResponseSize
failureReason
validationErrorCount
```

토큰 사용량은 AI 서버 응답 메타데이터에 포함될 때 저장한다.

```text
promptTokens
completionTokens
totalTokens
```

## 15. 팀 채팅 소통 기능 보완

### 목적

팀 배정 이후 채팅만으로는 중요한 정보와 역할을 관리하기 어려우므로, 최소한의 운영 기능을 추가한다.

### 메시지 고정

API 초안:

```text
PATCH /api/chat/messages/{messageId}/pin
DELETE /api/chat/messages/{messageId}/pin
GET /api/chat/channels/{channelId}/pinned-message
```

엔티티 변경 후보:

```text
ChatMessage
- pinned: boolean
- pinnedAt: LocalDateTime
- pinnedBy: User
```

채널당 고정 메시지를 하나만 허용할지, 여러 개 허용할지 먼저 결정해야 한다. 초기 구현은 채널당 1개가 단순하다.

### 읽음 상태

현재 `ChatReadStatus` 엔티티가 이미 있으므로 재사용한다.

확인할 것:

- channelId별 마지막 읽은 messageId 저장 여부
- 팀원별 readAt 표시 가능 여부
- 메시지별 읽은 사람 수 계산 가능 여부

필요 API:

```text
POST /api/chat/channels/{channelId}/read
GET /api/chat/messages/{messageId}/read-status
```

### 팀원 담당 업무

간단한 방식은 `TeamUser`에 담당 업무 필드를 추가하는 것이다.

```text
TeamUser
- responsibility: String
```

API 초안:

```text
PATCH /api/teams/my-team/members/{userId}/responsibility
PATCH /api/admin/teams/{teamId}/members/{userId}/responsibility
```

권한:

- 학생: 본인 팀 내 담당 업무 수정 가능 여부 결정 필요
- 관리자: 모든 팀 담당 업무 수정 가능

## 16. 로그인 후 캡스톤/해커톤 선택 분기

### 목적

로그인 후 캡스톤과 해커톤 서비스를 선택해서 들어가게 하는 아이디어다.

백엔드 관점에서는 단순 라우팅 문제가 아니라 데이터 스코프가 나뉘는 문제다.

### 검토할 설계

서비스 타입 enum 추가:

```text
ProjectMode
- CAPSTONE
- HACKATHON
```

또는 더 일반적으로:

```text
ProgramType
- CAPSTONE
- HACKATHON
```

적용 후보:

- User 설문 데이터가 프로그램별로 분리되어야 하는지
- Team/TeamUser가 프로그램별로 분리되어야 하는지
- Notice/Journal/Chat이 프로그램별로 분리되어야 하는지
- AI matching job이 프로그램별 조건을 가져야 하는지

초기에는 화면 선택만 하고 같은 데이터 구조를 쓰는 것도 가능하지만, 실제 운영에서 캡스톤과 해커톤 데이터가 섞이면 문제가 크다. 구현 전 데이터 분리 범위를 먼저 정해야 한다.

## 17. 구현 우선순위

백엔드 기준 권장 순서는 다음과 같다.

### 1단계: 공통 기반

1. `FcmToken` 엔티티/Repository/Service
2. `NotificationLog` 엔티티/Repository
3. Firebase Admin SDK 설정
4. `NotificationService` 공통 발송 구조

이 단계가 끝나면 일지, 공지, 채팅 알림이 모두 같은 기반을 사용할 수 있다.

### 2단계: 알림 기능

1. 일지 마감 30분 전 자동 알림
2. 공지 등록 후 알림
3. 채팅 메시지 알림

설문 미응답 FCM 알림은 제외한다.

### 3단계: 학생 검색/설문 개선

1. 학생 검색 API
2. 선호 팀원 `userId` 검증
3. 기존 선호 팀원 데이터 호환성 확인

### 4단계: 팀 추천 버전 관리

1. `TeamRecommendationVersion` 추가
2. 추천 결과 저장 시 version 연결
3. 재생성 결과 새 버전 저장
4. 적용 전 기존 결과 보호
5. diff 계산 API

### 5단계: 직접 팀 구성

1. 수동 팀 구성 요청 DTO
2. 팀 구성 검증 서비스
3. 수동 구성 결과를 `MANUAL` 버전으로 저장
4. apply 시 실제 팀 반영

### 6단계: AI 안정성/진행률

1. `MatchingJob` 진행률 필드 확장
2. job 상태 응답 DTO 확장
3. AI 결과 검증 서비스
4. 처리 시간/실패율/토큰 사용량 로깅

### 7단계: 팀 소통 기능

1. 메시지 고정
2. 읽음 상태 상세 조회
3. 팀원 담당 업무 필드

## 18. 구현 시 주의할 점

### 기존 변경사항 보호

현재 코드베이스에 이미 matching job, team recommendation, chat presence 관련 구조가 있으므로 새로 만들기 전에 기존 클래스를 먼저 확인한다.

우선 확인할 파일:

```text
Backend/src/main/java/com/capteam/gaobackend/service/MatchingJobService.java
Backend/src/main/java/com/capteam/gaobackend/service/MatchingJobWorker.java
Backend/src/main/java/com/capteam/gaobackend/service/MatchingJobStateService.java
Backend/src/main/java/com/capteam/gaobackend/entity/MatchingJob.java
Backend/src/main/java/com/capteam/gaobackend/entity/TeamRecommendation.java
Backend/src/main/java/com/capteam/gaobackend/entity/TeamRecommendationMember.java
Backend/src/main/java/com/capteam/gaobackend/service/admin/AdminTeamRecommendationService.java
Backend/src/main/java/com/capteam/gaobackend/service/admin/AdminTeamRecommendationPersistenceService.java
Backend/src/main/java/com/capteam/gaobackend/service/ChatService.java
Backend/src/main/java/com/capteam/gaobackend/service/ChatPresenceService.java
Backend/src/main/java/com/capteam/gaobackend/entity/ChatReadStatus.java
Backend/src/main/java/com/capteam/gaobackend/service/JournalService.java
Backend/src/main/java/com/capteam/gaobackend/service/admin/AdminJournalService.java
Backend/src/main/java/com/capteam/gaobackend/service/admin/AdminNoticeService.java
```

### 알림 실패 처리

알림 실패 때문에 원래 기능이 실패하면 안 된다.

예:

- 공지 생성은 성공했는데 FCM 실패 -> 공지 생성 성공, 알림 로그만 FAILED
- 채팅 메시지 저장은 성공했는데 FCM 실패 -> 채팅 성공, 알림 로그만 FAILED
- 일지 알림 실패 -> 다음 스케줄에서 재시도 정책 검토

### 보안

- Firebase 서비스 계정 JSON은 절대 Git에 커밋하지 않는다.
- FCM 토큰은 인증된 사용자만 등록/삭제할 수 있다.
- 다른 사용자의 토큰을 임의로 등록하지 못하게 현재 로그인 사용자 기준으로 저장한다.
- 푸시 payload에 민감정보를 넣지 않는다.

### 트랜잭션

공지 생성 후 알림처럼 "저장 성공 후 발송"이 필요한 기능은 `AFTER_COMMIT` 이벤트 방식을 우선 고려한다.

팀 버전 apply처럼 여러 테이블을 바꾸는 기능은 하나의 트랜잭션으로 묶어야 한다.

### 테스트

추가하면 좋은 테스트:

- FCM 토큰 중복 등록 시 updatedAt 갱신
- FCM 토큰 삭제
- 일지 제출자 제외 검증
- 이미 발송한 일지 알림 중복 방지
- 공지 생성 실패 시 알림 미발송
- 채팅 발신자에게 알림 미발송
- 선호 팀원 본인/중복/최대 3명 검증
- 팀 버전 diff 계산
- 직접 팀 구성 중복 배정/정원 초과 검증
- AI 결과 누락/중복 검증

## 19. 새 채팅에서 바로 이어갈 때 첫 작업 추천

새 채팅에서 구현을 시작한다면 아래 순서로 말하면 된다.

```text
Backend/docs/maintenance-backend-plan.md 문서를 기준으로,
설문 미응답 FCM 알림은 제외하고,
먼저 FCM 공통 기반(FcmToken, NotificationLog, Firebase 설정, NotificationService)부터 구현해줘.
```

그 다음 작업:

```text
이제 일지 마감 30분 전 자동 FCM 알림을 구현해줘.
```

그 다음 작업:

```text
공지 등록 후 학생들에게 FCM 알림이 가도록 구현해줘.
```

그 다음 작업:

```text
채팅 메시지 도착 시 오프라인 사용자에게 FCM 알림을 보내도록 구현해줘.
```

알림 기반이 끝난 뒤:

```text
학생 검색 API와 선호 팀원 userId 검증을 구현해줘.
```

팀 추천 개선으로 넘어갈 때:

```text
팀 추천 결과 버전 저장과 재생성 전후 diff API를 구현해줘.
```
