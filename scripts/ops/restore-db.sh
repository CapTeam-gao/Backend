#!/usr/bin/env bash

set -Eeuo pipefail

container="${MYSQL_CONTAINER:-gao-mysql}"
backup_file="${1:-}"

if [[ -z "$backup_file" || ! -f "$backup_file" ]]; then
  echo "Usage: CONFIRM_RESTORE=YES $0 <backup.sql.gz>" >&2
  exit 1
fi

if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo "Restore is destructive. Set CONFIRM_RESTORE=YES to continue." >&2
  exit 1
fi

if ! docker inspect --format '{{.State.Running}}' "$container" 2>/dev/null | grep -q '^true$'; then
  echo "MySQL container is not running: $container" >&2
  exit 1
fi

gzip -t "$backup_file"

if [[ "${SKIP_PRE_RESTORE_BACKUP:-}" != "YES" ]]; then
  "$(dirname "$0")/backup-db.sh"
fi

gzip -dc "$backup_file" | docker exec -i "$container" sh -c '
  exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"
'

echo "Restore completed: $backup_file"
