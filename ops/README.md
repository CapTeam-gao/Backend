# CapTeam Operations

## Health check

Run on the EC2 host:

```bash
cd /home/ec2-user/apps/Backend
./scripts/ops/smoke-test.sh
```

The command checks all three containers, the public service processes, both
Backend/AI network directions, and the MySQL user count.

## Database backup

The `gao-db-backup.timer` unit runs every day at 03:30 Asia/Seoul and retains
14 days of compressed backups in `/home/ec2-user/backups/mysql`.

Create an extra backup manually:

```bash
/home/ec2-user/apps/Backend/scripts/ops/backup-db.sh
```

Restore only during a maintenance window:

```bash
CONFIRM_RESTORE=YES \
  /home/ec2-user/apps/Backend/scripts/ops/restore-db.sh \
  /home/ec2-user/backups/mysql/gao-db-YYYYMMDD-HHMMSS.sql.gz
```

The restore command creates a pre-restore backup by default.

## Production cutover

1. Stop writes to the old service.
2. Export the old `gao_db` and import it with `restore-db.sh`.
3. Run `smoke-test.sh` and compare key table counts.
4. Point `api.capteam.n-e.kr` to the new EC2 public IP.
5. Issue the TLS certificate and verify the HTTPS health endpoint.
6. Run login, survey, AI analysis, and team matching tests with test accounts.
