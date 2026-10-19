#!/usr/bin/env bash

set -Eeuo pipefail

container="${MYSQL_CONTAINER:-gao-mysql}"
backup_dir="${1:-${BACKUP_DIR:-$HOME/backups/mysql}}"
retention_days="${BACKUP_RETENTION_DAYS:-14}"
timestamp="$(date +%Y%m%d-%H%M%S)"
backup_file="$backup_dir/gao-db-$timestamp.sql.gz"
temporary_file="$backup_file.tmp"

cleanup() {
  rm -f "$temporary_file"
}
trap cleanup EXIT

if ! docker inspect --format '{{.State.Running}}' "$container" 2>/dev/null | grep -q '^true$'; then
  echo "MySQL container is not running: $container" >&2
  exit 1
fi

mkdir -p "$backup_dir"
chmod 700 "$backup_dir"

docker exec "$container" sh -c '
  exec mysqldump \
    -uroot \
    -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --events \
    --hex-blob \
    --set-gtid-purged=OFF \
    "$MYSQL_DATABASE"
' | gzip -9 > "$temporary_file"

test -s "$temporary_file"
gzip -t "$temporary_file"
chmod 600 "$temporary_file"
mv "$temporary_file" "$backup_file"

find "$backup_dir" -type f -name 'gao-db-*.sql.gz' -mtime "+$retention_days" -delete

echo "$backup_file"
