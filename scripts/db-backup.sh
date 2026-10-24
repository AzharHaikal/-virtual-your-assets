#!/usr/bin/env sh
# Daily logical backup of the VYRA database (all application schemas + Flyway history).
#
#   PGHOST=... PGPORT=5432 PGUSER=... PGPASSWORD=... PGDATABASE=vyra_assets \
#   BACKUP_DIR=/backups RETENTION_DAYS=7 ./scripts/db-backup.sh
#
# Output: $BACKUP_DIR/vyra_<db>_<UTC timestamp>.dump (custom format, restore with pg_restore).
# Files older than RETENTION_DAYS are deleted. Schedule it once a day (cron, CI schedule, ...).
set -eu

: "${PGDATABASE:?set PGDATABASE}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
file="$BACKUP_DIR/vyra_${PGDATABASE}_$(date -u +%Y%m%dT%H%M%SZ).dump"

pg_dump --format=custom --no-owner --no-privileges \
        --schema=flyway --schema=admin_idp --schema=idp --schema=wallet --schema=transaction_finance \
        --schema=wellbeing --schema=notification \
        --file="$file"

# A dump that pg_restore cannot list is not a backup.
pg_restore --list "$file" > /dev/null
echo "backup written: $file ($(wc -c < "$file") bytes)"

find "$BACKUP_DIR" -name "vyra_${PGDATABASE}_*.dump" -type f -mtime +"$RETENTION_DAYS" -print -delete
