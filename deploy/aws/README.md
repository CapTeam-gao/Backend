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
- MySQL 3306과 Spring Boot 8080은 외부에 열지 않습니다.
- Docker Engine, Docker Compose plugin, Nginx를 설치합니다.

```bash
cp .env.example .env
chmod 600 .env
```

`.env`에서 비밀번호, JWT Secret, 프론트 Origin, S3 버킷명을 실제 값으로 교체합니다.

```bash
docker compose config
docker compose build
docker compose up -d
docker compose ps
curl --fail http://127.0.0.1:8080/actuator/health
```

## 4. Nginx와 HTTPS

`deploy/nginx/gao-backend.conf`의 `api.example.com`을 실제 API 도메인으로 변경해 Nginx에 설치합니다. 이 설정은 WebSocket Upgrade 헤더와 업로드 크기 20MB를 포함합니다.

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

## 5. 동작 확인

1. 로그인한 팀원이 `/api/chat/channels/{channelId}/files`에 파일을 업로드합니다.
2. 응답의 `fileUrl`이 `/chat-files/{channelId}/{storedFileName}`인지 확인합니다.
3. 같은 팀 사용자가 Authorization 헤더와 함께 `fileUrl`을 요청하면 HTTP 302로 Presigned S3 URL에 이동합니다.
4. 다른 팀 사용자는 채널 권한 검사에서 거부되는지 확인합니다.
5. S3 객체 키가 `chat/channels/{channelId}/...` 형태이고 퍼블릭 URL로 직접 열리지 않는지 확인합니다.

## 배포 전 전제 조건

- `spring.jpa.hibernate.ddl-auto=validate`이므로 운영 DB 스키마가 엔티티와 일치해야 합니다. 현재 저장소에는 Flyway/Liquibase 마이그레이션이 없으므로 빈 DB에 바로 배포하면 시작되지 않습니다.
- AI 서버가 같은 EC2 호스트에서 실행되면 `host.docker.internal:8000`을 사용하고, 별도 서버면 `AI_SERVER_BASE_URL`을 내부 또는 HTTPS 주소로 변경합니다.
- EC2 한 대의 내장 WebSocket broker 구성입니다. 여러 EC2로 수평 확장할 때는 외부 broker relay와 세션 라우팅 구성이 추가로 필요합니다.
