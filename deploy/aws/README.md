# EC2 + S3 배포 가이드

## 1. S3 버킷

- 백엔드와 같은 리전(기본 `ap-northeast-2`)에 일반 목적 버킷을 생성합니다.
- Block Public Access 네 항목을 모두 활성화합니다.
- Object Ownership은 `Bucket owner enforced`를 사용합니다.
- 기본 암호화는 SSE-S3를 사용합니다. 애플리케이션도 업로드 요청에 AES256 암호화를 명시합니다.
- 브라우저에서 Presigned URL을 `fetch`로 읽는 경우 `s3-cors.json`의 Origin을 실제 프론트 주소로 바꿔 적용합니다.

```bash
aws s3api put-bucket-cors \
  --bucket YOUR_BUCKET_NAME \
  --cors-configuration file://deploy/aws/s3-cors.json
```

## 2. EC2 IAM Role

1. 신뢰 주체가 EC2인 IAM Role을 생성합니다.
2. `ec2-s3-policy.json`의 `YOUR_BUCKET_NAME`을 실제 버킷명으로 바꿔 Role에 연결합니다.
3. EC2 인스턴스의 IAM Role로 연결합니다.
4. Access Key와 Secret Key는 EC2나 `.env`에 저장하지 않습니다.

Docker bridge 내부에서도 IMDSv2 응답을 받을 수 있도록 EC2 Metadata options를 다음처럼 설정합니다.

```bash
aws ec2 modify-instance-metadata-options \
  --instance-id i-xxxxxxxxxxxxxxxxx \
  --http-endpoint enabled \
  --http-tokens required \
  --http-put-response-hop-limit 2
```

## 3. EC2 준비

- Security Group 인바운드는 SSH 관리 IP, HTTP 80, HTTPS 443만 허용합니다.
- MySQL 3306, Spring Boot 8080, AI 8000은 외부에 열지 않습니다.
- Docker Engine, Docker Compose plugin, Nginx를 설치합니다.
- 배포 사용자를 `docker` 그룹에 추가한 뒤 다시 로그인합니다.

```bash
cp .env.example .env
chmod 600 .env
```

`.env`에서 비밀번호, JWT Secret, 프론트 Origin, S3 버킷명을 실제 값으로 교체합니다.

```bash
docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
curl --fail http://127.0.0.1:8080/actuator/health
```

백엔드 Compose가 `backend_internal` 네트워크와 MySQL 볼륨을 생성합니다. 따라서 같은 EC2에 배포할 때는 백엔드를 먼저 배포하고 AI를 나중에 배포합니다.

## 4. GitHub Actions Secrets

Backend 저장소에는 다음 Actions Secrets를 등록합니다.

```text
EC2_HOST, EC2_USER, EC2_PORT, EC2_SSH_KEY
MYSQL_ROOT_PASSWORD, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD
SPRING_DATASOURCE_URL
JWT_SECRET, JWT_ACCESS_TOKEN_EXPIRATION, JWT_REFRESH_TOKEN_EXPIRATION
JWT_REFRESH_COOKIE_SECURE, JWT_REFRESH_COOKIE_SAME_SITE
WEBSOCKET_ALLOWED_ORIGINS
AI_SERVER_BASE_URL, INTERNAL_MATCHING_API_KEY
FIREBASE_ENABLED, FIREBASE_PROJECT_ID, FIREBASE_CREDENTIALS_JSON
AWS_REGION, AWS_S3_BUCKET, AWS_S3_KEY_PREFIX, AWS_S3_PRESIGNED_URL_DURATION
CHAT_FILE_STORAGE_TYPE, CHAT_FILE_LOCAL_ROOT
CHAT_FILE_MAX_SIZE, CHAT_FILE_MAX_REQUEST_SIZE
BACKEND_BIND_ADDRESS, BACKEND_PORT, BACKEND_CPUS, MYSQL_CPUS
```

권장 핵심 값은 다음과 같습니다.

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://gao-mysql:3306/gao_db?serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci
AI_SERVER_BASE_URL=http://ai-server:8000
BACKEND_BIND_ADDRESS=127.0.0.1
BACKEND_PORT=8080
JWT_REFRESH_COOKIE_SECURE=true
JWT_REFRESH_COOKIE_SAME_SITE=None
WEBSOCKET_ALLOWED_ORIGINS=https://capteam.n-e.kr
CHAT_FILE_STORAGE_TYPE=s3
```

`FIREBASE_ENABLED=true`이면 `FIREBASE_PROJECT_ID`와 `FIREBASE_CREDENTIALS_JSON`이 필수입니다. JSON은 서비스 계정 파일을 한 줄 JSON으로 변환해 Secret에 저장합니다. `INTERNAL_MATCHING_API_KEY`는 AI 저장소 Secret과 완전히 같아야 합니다.

## 5. Nginx와 HTTPS

`deploy/nginx/gao-backend.conf`를 Nginx에 설치합니다. 이 설정은 `api.capteam.n-e.kr`, WebSocket Upgrade 헤더와 업로드 크기 20MB를 포함합니다.

```bash
sudo cp deploy/nginx/gao-backend.conf /etc/nginx/sites-available/gao-backend
sudo ln -s /etc/nginx/sites-available/gao-backend /etc/nginx/sites-enabled/gao-backend
sudo nginx -t
sudo systemctl reload nginx
```

DNS A 레코드를 EC2 Elastic IP로 연결한 뒤 Certbot 등으로 TLS 인증서를 적용합니다. HTTPS 적용 시 `.env`는 다음 값을 사용합니다.

```properties
JWT_REFRESH_COOKIE_SECURE=true
JWT_REFRESH_COOKIE_SAME_SITE=None
WEBSOCKET_ALLOWED_ORIGINS=https://frontend.example.com
```

## 6. 배포 순서와 동작 확인

1. Backend 저장소의 `main` 또는 `master`를 배포합니다.
2. `gao-backend`, `gao-mysql`이 healthy인지 확인합니다.
3. AI 저장소의 `main`을 배포합니다.
4. `ai-server`가 healthy인지 확인합니다.
5. DNS와 TLS 적용 후 외부 health endpoint를 확인합니다.

```bash
docker compose -f /home/$USER/apps/Backend/docker-compose.prod.yml ps
docker logs --tail=200 gao-backend
docker logs --tail=200 ai-server
curl --fail http://127.0.0.1:8080/actuator/health
curl --fail http://127.0.0.1:8000/health
curl --fail https://api.capteam.n-e.kr/actuator/health
```

애플리케이션 검증은 다음 순서로 진행합니다.

1. 로그인한 팀원이 `/api/chat/channels/{channelId}/files`에 파일을 업로드합니다.
2. 응답의 `fileUrl`이 `/chat-files/{channelId}/{storedFileName}`인지 확인합니다.
3. 같은 팀 사용자가 Authorization 헤더와 함께 `fileUrl`을 요청하면 HTTP 302로 Presigned S3 URL에 이동합니다.
4. 다른 팀 사용자는 채널 권한 검사에서 거부되는지 확인합니다.
5. S3 객체 키가 `chat/channels/{channelId}/...` 형태이고 퍼블릭 URL로 직접 열리지 않는지 확인합니다.

## 배포 전 전제 조건

- 현재 `spring.jpa.hibernate.ddl-auto=update`입니다. 첫 실행 시 스키마가 자동 갱신되므로 기존 DB를 옮길 때는 반드시 먼저 백업합니다.
- Backend와 AI는 `backend_internal` Docker 네트워크에서 각각 `gao-backend:8080`, `ai-server:8000`으로 통신합니다.
- EC2 한 대의 내장 WebSocket broker 구성입니다. 여러 EC2로 수평 확장할 때는 외부 broker relay와 세션 라우팅 구성이 추가로 필요합니다.
