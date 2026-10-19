#!/usr/bin/env bash

set -Eeuo pipefail
umask 077

api_base_url="${API_BASE_URL:-http://127.0.0.1:8080}"
matching_timeout_seconds="${MATCHING_TIMEOUT_SECONDS:-900}"
poll_interval_seconds="${MATCHING_POLL_INTERVAL_SECONDS:-5}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command is missing: $1" >&2
    exit 1
  }
}

require_value() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Required environment variable is missing: $name" >&2
    exit 1
  fi
}

request() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"
  local args=(
    --fail-with-body
    --silent
    --show-error
    --request "$method"
    --header "Accept: application/json"
  )

  if [[ -n "$token" ]]; then
    args+=(--header "Authorization: Bearer $token")
  fi
  if [[ -n "$body" ]]; then
    args+=(--header "Content-Type: application/json" --data "$body")
  fi

  curl "${args[@]}" "$api_base_url$path"
}

login() {
  local user_id="$1"
  local password="$2"
  local payload
  payload="$(jq -cn --arg userId "$user_id" --arg password "$password" \
    '{userId: $userId, password: $password}')"
  request POST "/api/auth/login" "" "$payload"
}

require_command curl
require_command jq
require_value SMOKE_USER_ID
require_value SMOKE_USER_PASSWORD

user_login_response="$(login "$SMOKE_USER_ID" "$SMOKE_USER_PASSWORD")"
user_token="$(jq -er '.accessToken | select(type == "string" and length > 0)' <<<"$user_login_response")"
user_role="$(jq -r '.role' <<<"$user_login_response")"

request GET "/api/auth/me" "$user_token" | jq -e '.isLoggedIn == true' >/dev/null
request GET "/api/user/header" "$user_token" | jq -e 'type == "object"' >/dev/null
request GET "/api/user/survey" "$user_token" | jq -e '.success == true and (.data | type == "object")' >/dev/null

search_keyword="${SMOKE_SEARCH_KEYWORD:-${SMOKE_USER_ID:0:3}}"
encoded_keyword="$(jq -rn --arg value "$search_keyword" '$value | @uri')"
request GET "/api/user/students/search?keyword=$encoded_keyword" "$user_token" \
  | jq -e '.success == true and (.data | type == "array")' >/dev/null

echo "Student login: ok ($user_role)"
echo "Authenticated status: ok"
echo "Header API: ok"
echo "Survey read API: ok"
echo "Preferred teammate search API: ok"

if [[ "${RUN_AI_MATCHING:-NO}" != "YES" ]]; then
  echo "AI matching: skipped (set RUN_AI_MATCHING=YES to run)"
  exit 0
fi

require_value SMOKE_ADMIN_ID
require_value SMOKE_ADMIN_PASSWORD

admin_login_response="$(login "$SMOKE_ADMIN_ID" "$SMOKE_ADMIN_PASSWORD")"
admin_token="$(jq -er '.accessToken | select(type == "string" and length > 0)' <<<"$admin_login_response")"
admin_role="$(jq -er '.role | select(. == "ADMIN")' <<<"$admin_login_response")"

request GET "/api/admin/students" "$admin_token" \
  | jq -e '.students | type == "array"' >/dev/null

matching_grade="${SMOKE_MATCHING_GRADE:-GRADE_2}"
matching_payload="$(jq -cn --arg grade "$matching_grade" \
  '{grade: $grade, regenerationPrompt: null, baseVersionId: null}')"
matching_response="$(request POST "/api/admin/team-recommendations/matching/run" "$admin_token" "$matching_payload")"
job_id="$(jq -er '.data.jobId | select(type == "string" and length > 0)' <<<"$matching_response")"

deadline=$((SECONDS + matching_timeout_seconds))
while (( SECONDS < deadline )); do
  job_response="$(request GET "/api/admin/team-recommendations/matching/jobs/$job_id" "$admin_token")"
  job_status="$(jq -er '.data.status' <<<"$job_response")"
  case "$job_status" in
    SUCCEEDED)
      version_id="$(jq -r '.data.versionId // ""' <<<"$job_response")"
      echo "Admin login: ok ($admin_role)"
      echo "Admin student API: ok"
      echo "AI matching: ok (jobId=$job_id, versionId=$version_id)"
      exit 0
      ;;
    FAILED|CANCELLED)
      error_message="$(jq -r '.data.errorMessage // "unknown error"' <<<"$job_response")"
      echo "AI matching failed: status=$job_status, message=$error_message" >&2
      exit 1
      ;;
  esac
  sleep "$poll_interval_seconds"
done

echo "AI matching timed out: jobId=$job_id, timeout=${matching_timeout_seconds}s" >&2
exit 1
