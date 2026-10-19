#!/usr/bin/env bash

set -Eeuo pipefail

backend_url="${BACKEND_URL:-http://127.0.0.1:8080}"
ai_url="${AI_URL:-http://127.0.0.1:8000}"
mysql_container="${MYSQL_CONTAINER:-gao-mysql}"
backend_container="${BACKEND_CONTAINER:-gao-backend}"
ai_container="${AI_CONTAINER:-ai-server}"

assert_healthy() {
  local container="$1"
  local status
  status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container")"
  if [[ "$status" != "healthy" && "$status" != "running" ]]; then
    echo "Container is not healthy: $container ($status)" >&2
    exit 1
  fi
  printf '%-16s %s\n' "$container" "$status"
}

assert_healthy "$mysql_container"
assert_healthy "$backend_container"
assert_healthy "$ai_container"

curl --fail --silent --show-error "$backend_url/actuator/health" >/dev/null
curl --fail --silent --show-error "$ai_url/health" >/dev/null

docker exec "$backend_container" curl --fail --silent --show-error \
  http://ai-server:8000/health >/dev/null

docker exec "$ai_container" python -c '
import urllib.request
urllib.request.urlopen("http://gao-backend:8080/actuator/health", timeout=5).read()
'

user_count="$(docker exec "$mysql_container" sh -c '
  exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse \
    "SELECT COUNT(*) FROM users" "$MYSQL_DATABASE"
')"

echo "Backend health: ok"
echo "AI health: ok"
echo "Backend -> AI: ok"
echo "AI -> Backend: ok"
echo "MySQL users: $user_count"
