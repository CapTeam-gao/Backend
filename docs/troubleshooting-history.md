# CapTeam 트러블슈팅 기록

이 문서는 CapTeam 개발 과정에서 실제로 확인했거나 대화에서 점검한 문제를 나중에 포트폴리오, 발표, 회고에 활용할 수 있도록 정리한 기록이다.

표기 기준:

- `해결`: 원인과 수정 방향을 확인하고 코드 또는 설정에 반영한 항목
- `점검 필요`: 원인은 좁혔지만 실제 운영 환경에서 전체 흐름 검증이 남은 항목
- API와 필드명을 변경할 때는 항상 프론트 실제 호출 코드, 백엔드 DTO/Controller, AI 응답 스키마를 함께 대조한다.

## 포트폴리오 우선 후보

1. MySQL 한글 데이터 깨짐 및 검색 문제
2. 트랜잭션 rollback-only 문제
3. AI-백엔드 `role_counts` 응답 스키마 불일치
4. 팀 재생성 버전 추적 문제

단순 의존성 누락, 관리자 `grade` 제약, API 405 문제는 보조 사례로 사용한다.

## 1. MySQL 한글 데이터 깨짐 및 검색 문제

상태: 해결

### 증상

- 배포 서버의 MySQL CLI에서 한글 학생 이름을 직접 입력하면 빈 값 또는 `???`처럼 보였다.
- 선호 팀원 검색에서 한글 이름으로 학생이 검색되지 않는 것처럼 보였다.

### 원인

- MySQL 서버, 데이터베이스, 테이블, JDBC 연결 및 CLI의 문자셋이 일관되지 않았다.
- 터미널에서 MySQL CLI로 한글을 붙여넣는 시점에 이미 문자가 손상될 수 있었다.
- `CONVERT(UNHEX(...) USING utf8mb4)` 자체는 검색 실패 원인이 아니다. 올바른 UTF-8 바이트라면 일반 문자열로 저장되므로 `LIKE` 검색이 가능하다.

### 확인 방법

```sql
SELECT user_id, name, HEX(name)
FROM users
WHERE user_id = '학생학번';
```

- 정상 UTF-8 HEX가 나오면 DB에는 정상 저장된 것이다.
- `3F3F3F`처럼 `3F`가 반복되면 실제 물음표가 저장된 것이다.

### 해결

- MySQL 서버와 JDBC 연결을 `utf8mb4`로 통일한다.
- MySQL CLI 접속 시 `--default-character-set=utf8mb4`를 사용한다.
- 배포 서버에서 직접 한글 테스트 데이터를 넣을 때는 UTF-8 HEX 방식을 사용한다.

```sql
CONVERT(UNHEX('UTF8_HEX_VALUE') USING utf8mb4)
```

### 포트폴리오 핵심

화면에 보이는 문자열만 확인하지 않고 `HEX()`로 저장된 바이트를 검사해, 출력 환경 문제와 실제 데이터 손상을 구분했다.

## 2. Transaction rollback-only 문제

상태: 점검 필요

### 증상

```text
Transaction silently rolled back because it has been marked as rollback-only
```

- 설문 저장 또는 AI 분석 처리 중 위 메시지가 발생했다.
- 내부 오류를 잡았는데도 메서드 종료 시 전체 트랜잭션이 롤백될 수 있었다.

### 원인

- 트랜잭션 내부에서 예외가 발생하면 해당 트랜잭션이 rollback-only로 표시될 수 있다.
- 이후 예외를 `catch`하여 정상 흐름처럼 반환해도 트랜잭션 상태는 복구되지 않는다.
- 최종 rollback-only 메시지는 2차 증상이며, 실제 원인은 로그에서 먼저 발생한 예외다.

### 해결 방향

- 설문 저장과 외부 AI 호출의 트랜잭션 경계를 분리한다.
- 핵심 데이터 저장이 끝난 뒤 AI 분석을 실행하거나, 별도 서비스/이벤트로 처리한다.
- 예외를 무시해야 하는 로직은 같은 트랜잭션 안에서 단순히 `catch`하지 않는다.
- AI 분석 실패가 설문 저장까지 롤백시키지 않도록 한다.

### 추가 검증

- 실제 실패 로그의 최초 예외를 기준으로 재현 테스트가 필요하다.
- 설문 저장 성공과 AI 분석 실패가 독립적으로 기록되는지 확인해야 한다.

### 포트폴리오 핵심

외부 시스템 실패와 핵심 비즈니스 데이터 저장을 분리하고, Spring 트랜잭션 전파와 예외 처리 범위를 재설계한 사례로 설명한다.

## 3. AI-백엔드 role_counts 응답 스키마 불일치

상태: 해결

### 증상

- 설문이 100% 완료된 상태에서도 AI 팀 생성이 실패했다.
- Jackson `MismatchedInputException`이 발생했다.

### 원인

- FastAPI는 `role_counts`를 JSON 객체로 반환했다.
- Spring Boot의 `AiTeamSummaryResponseDto.TeamDto.roleCounts`는 `List<RoleCountDto>`를 기대했다.
- 두 서버가 같은 필드명에 서로 다른 자료형을 사용해 역직렬화가 실패했다.

### 해결

- AI 응답에서 `role_counts`를 정규화했다.
- 백엔드에는 `RoleCountsDeserializer`를 적용해 객체와 배열 형태를 안전하게 처리했다.
- 이후에도 AI 응답 필드와 백엔드 DTO를 계약 기준으로 함께 검증한다.

### 포트폴리오 핵심

단순 네트워크 실패로 보이던 문제를 응답 역직렬화 단계까지 추적하고, 외부 API 계약 변화에 대응할 수 있도록 경계 계층을 보강했다.

## 4. 팀 재생성 버전 추적 문제

상태: 코드 반영, E2E 점검 필요

### 증상

- 팀 재생성을 실행해도 프론트에서 이전 결과와의 변경사항 버튼이 나타나지 않았다.
- 재생성이 신규 생성처럼 처리됐다.

### 원인

- 프론트는 매칭 작업 응답에서 `versionId`, `baseVersionId`, `origin`을 기대했다.
- `MatchingJobResponseDto`에 해당 필드가 없어서 생성된 버전과 작업을 연결할 수 없었다.

### 해결

- `MatchingJob`에 `baseVersionId`를 추가했다.
- `MatchingJobResponseDto`에 `versionId`, `baseVersionId`, `origin`을 추가했다.
- `TeamMatchingVersion.jobId`로 작업과 생성된 버전을 연결했다.
- 최초 생성은 `INITIAL`, 재생성은 `REGENERATION`으로 구분했다.

### 추가 검증

- 생성 완료 후 polling 응답에 `versionId`가 포함되는지 확인한다.
- 이전 버전과 신규 버전 diff, 적용, 폐기까지 실제 화면에서 검증한다.

### 포트폴리오 핵심

비동기 작업과 결과 버전 사이의 추적 식별자를 설계해, 재생성 결과를 비교·적용·폐기할 수 있도록 상태 모델을 확장했다.

## 5. 선호 팀원 검색 API 경로 및 검증

상태: 해결

### 증상

- 프론트에서 학생 검색 API가 404를 반환했다.
- 검색 결과만 조작하면 본인, 중복 학생 또는 3명을 초과한 학생을 저장할 가능성이 있었다.

### 원인

- 실제 `UserController`의 베이스 경로는 `/api/user`인데 다른 경로로 호출한 이력이 있었다.
- 프론트 입력 제한만으로는 서버 데이터의 무결성을 보장할 수 없다.

### 해결

- 학생 검색 경로를 `GET /api/user/students/search?keyword=...`로 맞췄다.
- 같은 학년, 본인 제외, 이름·학번 일부 검색, 최대 6명 반환 조건을 적용했다.
- 설문 저장 시 존재 여부, 본인 제외, 중복 제외, 최대 3명을 백엔드에서 다시 검증했다.

## 6. 관리자 직접 팀 구성 API 405

상태: 해결

### 증상

- 프론트가 `POST /api/admin/teams/manual`을 호출하면 `405 Method Not Allowed`가 발생했다.

### 원인

- 프론트가 사용하는 경로에 대응하는 백엔드 POST 매핑이 없었다.

### 해결

- 프론트 호환 엔드포인트를 추가했다.
- 최대 5명, 학생 중복 배정 금지, 학년 일치, 팀장 지정, 기존 확정 팀 중복을 서버에서 검증했다.
- 직접 구성 완료 시 실제 팀과 기본 채팅방이 생성되도록 연결했다.

## 7. 관리자 계정 grade NOT NULL 문제

상태: 원인 확인

### 증상

```text
ERROR 1048 (23000): Column 'grade' cannot be null
```

- 관리자 계정 INSERT에서 `grade = NULL`을 사용할 수 없었다.

### 원인

- 애플리케이션에서는 관리자에게 학년이 필요 없지만 기존 MySQL 스키마의 `users.grade`가 `NOT NULL`이었다.
- Docker 이미지를 다시 빌드하지 않아서 생긴 문제가 아니라 기존 DB 볼륨에 남아 있는 스키마 제약 문제다.

### 해결 선택지

- 임시 운영 데이터에는 관리자 계정에도 유효한 학년을 넣는다.
- 올바른 장기 해결은 엔티티와 DB 마이그레이션을 함께 수정해 관리자 `grade`를 nullable로 허용하는 것이다.

## 8. Firebase 프론트 의존성 누락

상태: 원인 확인

### 증상

```text
Failed to resolve import "firebase/app" from "src/firebase/messaging.js"
```

### 원인

- FCM 코드가 `firebase/app`, `firebase/messaging`을 import하지만 현재 `node_modules`에 Firebase SDK가 없었다.
- 프론트 코드를 새로 가져온 뒤 의존성 설치 상태가 소스와 맞지 않았다.

### 해결

- `package.json`에 Firebase 의존성이 있는지 확인하고 패키지를 설치한다.
- FCM을 비활성화하더라도 해당 모듈을 정적으로 import하면 빌드 시 패키지가 필요하다.

## 9. FCM 구현 상태 오판

상태: 정정

### 상황

- 이전 코드나 프론트 보고서만 기준으로 FCM API가 없다고 판단한 적이 있었다.
- 새로 가져온 백엔드에는 토큰 등록 기반 코드가 이미 존재했다.

### 확인된 구성

- `POST /api/user/fcm-token`
- `DELETE /api/user/fcm-token`
- `UserFcmToken`
- `FcmTokenService`
- `FirebasePushNotificationGateway`

### 교훈

- FCM 토큰 인프라가 있다는 것과 일지·공지·채팅 알림 트리거가 완성됐다는 것은 별개다.
- 구현 상태를 판단하기 전에 실제 Controller, Service, Entity, 호출 지점을 모두 확인한다.

## 10. 로그인 상태 확인 화면에서 멈춤

상태: 점검 기준 정리

### 증상

- 화면이 `로그인 상태를 확인하는 중입니다.`에서 진행되지 않았다.

### 점검 항목

- 인증 확인 API는 `GET /api/auth/me`다.
- 백엔드 실행 여부와 API base URL을 확인한다.
- Access Token 또는 refresh cookie 전달 여부를 확인한다.
- CORS 및 cookie의 Secure/SameSite 설정을 확인한다.
- CapTeam 프론트 개발 포트는 `5174`다.

## 11. AI 분석 결과가 조회되지 않는 문제

상태: 일부 반영, 실패 상태 영속화 필요

### 증상

- 설문은 저장됐지만 `분석 설명을 아직 조회할 수 없습니다`가 표시됐다.

### 가능한 원인

- `user_analysis.analysis_result`가 생성되지 않았다.
- AI 응답, 백엔드 DTO, 프론트가 서로 다른 필드명을 사용했다.
- 분석 진행 상태가 없어 프론트가 성공·진행·실패를 구분할 수 없었다.

### 반영 내용

- `AdminStudentDetailResponseDto`에 `analysisStatus`를 추가했다.
- 설문 미완료 또는 결과 없음은 `PENDING`, 결과가 있으면 `SUCCESS`로 구분한다.

### 추가 검증

- 실제 `FAILED`를 표현하려면 AI 호출 실패 상태와 사유를 DB에 저장해야 한다.

## 12. WebSocket 채팅 SEND 경로 점검

상태: 경로 확인, 실제 전송 E2E 점검 필요

### 증상

- WebSocket CONNECT와 SUBSCRIBE는 성공하지만 백엔드에 SEND 로그가 나타나지 않았다.

### 확인된 경로

- 백엔드 핸들러: `@MessageMapping("/chat/{channelId}/send")`
- 애플리케이션 destination prefix: `/pub`
- 프론트 전송 목적지: `/pub/chat/{channelId}/send`

### 점검 항목

- 프론트 destination과 channelId를 확인한다.
- STOMP 연결 완료 후 전송하는지 확인한다.
- payload 필드가 백엔드 요청 DTO와 일치하는지 확인한다.
- 채팅 WebSocket 알림과 FCM 푸시 알림은 별도 흐름으로 검증한다.

## 13. Docker 재시작과 DB 데이터 보존

상태: 운영 방법 정리

- 소스나 Docker 이미지를 수정했으면 백엔드 컨테이너를 재시작하거나 다시 빌드해야 반영된다.
- `docker compose down` 후 `docker compose up -d --build`는 named volume을 명시적으로 삭제하지 않는 한 DB 데이터를 유지한다.
- `docker compose down -v`는 MySQL 볼륨을 삭제하므로 데이터 보존이 필요할 때 사용하지 않는다.
- 재배포 전에 DB 백업과 현재 volume 목록을 확인한다.

## 14. 프로젝트 경로 변경

현재 실제 프로젝트 경로:

```text
/Users/anyaswin1/Desktop/springboot/CapStone/CapTeam
```

이전 경로 `/Users/anyaswin1/Desktop/springboot/CapTeam`을 기준으로 명령을 실행하면 디렉터리를 찾을 수 없다.

## 포트폴리오 작성 형식

각 사례는 다음 순서로 작성한다.

1. 문제 상황과 사용자 영향
2. 최초 가설과 실제 원인
3. 로그, SQL, 응답 payload 등 확인 근거
4. 적용한 해결 방법과 선택 이유
5. 해결 결과와 검증 방법
6. 재발 방지를 위한 구조 개선

성과를 과장하지 않는다. 실제 E2E 검증이 끝나지 않은 항목은 `코드 반영 후 통합 테스트 진행 중`으로 표현한다.
