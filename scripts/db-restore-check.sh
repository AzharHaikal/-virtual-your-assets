#!/usr/bin/env sh
# Restore test: loads a backup into a NEW scratch database and checks it is usable.
# Never point it at the production database: the target database is created and must not exist.
#
#   PGHOST=... PGPORT=5432 PGUSER=... PGPASSWORD=... \
#   ./scripts/db-restore-check.sh backups/vyra_vyra_assets_20261022T000000Z.dump vyra_restore_check
#
# Afterwards start the application against it with ddl-auto=validate (default) to confirm the schema:
#   DATABASE_URL=jdbc:postgresql://$PGHOST:$PGPORT/vyra_restore_check ... java -jar app.jar
set -eu

dump="${1:?usage: db-restore-check.sh <dump file> <scratch database name>}"
target="${2:?usage: db-restore-check.sh <dump file> <scratch database name>}"

createdb "$target"
pg_restore --no-owner --no-privileges --exit-on-error --dbname="$target" "$dump"

echo "Flyway version:"
psql --dbname="$target" -Atc "SELECT max(version::int) FROM flyway.flyway_schema_history WHERE success"

echo "Row counts:"
psql --dbname="$target" -Atc "
  SELECT 'members',      count(*) FROM idp.member
  UNION ALL SELECT 'transactions', count(*) FROM transaction_finance.transaction
  UNION ALL SELECT 'wallets',      count(*) FROM wallet.wallet_statement
  UNION ALL SELECT 'admins',       count(*) FROM admin_idp.admin"

echo "restore check OK: $target (drop it when done: dropdb $target)"
