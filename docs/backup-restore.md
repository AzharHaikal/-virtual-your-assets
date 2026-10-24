# Backup & Restore — VYRA Database

Target (PRD NFR-08): scheduled backups and a restore procedure that is **tested**.

## 1. What is backed up

One logical dump (`pg_dump --format=custom`) of every application schema:
`flyway`, `admin_idp`, `idp`, `wallet`, `transaction_finance`, `wellbeing` (mood & journal, habits, goals), `notification`.
The wallet balance and the journal stay encrypted in the dump (AES-256-GCM): a restore is only usable with the same `CRYPTO_SECRET_KEY`, so keep that key in the secret store, **never next to the backups**.

## 2. Schedule

| Item | Rule |
|---|---|
| Frequency | Daily (plus the provider's automatic snapshots if the hosting offers them) |
| Retention | At least 7 days (`RETENTION_DAYS`) |
| Storage | Outside the database host (object storage / another machine), private |
| Before risky releases | Extra manual backup (e.g. before a migration such as V7) |

Run it with [`scripts/db-backup.sh`](../scripts/db-backup.sh):

```bash
PGHOST=... PGPORT=5432 PGUSER=... PGPASSWORD=... PGDATABASE=vyra_assets \
BACKUP_DIR=/backups RETENTION_DAYS=7 ./scripts/db-backup.sh
```

Schedule it with cron on a trusted machine (`0 18 * * *` UTC = 01:00 WIB) or a scheduled CI job whose credentials come from protected CI variables.

## 3. Restore test (at least once a month)

```bash
PGHOST=... PGUSER=... PGPASSWORD=... \
./scripts/db-restore-check.sh /backups/vyra_vyra_assets_<timestamp>.dump vyra_restore_check
```

The script creates a new scratch database, restores into it and prints the Flyway version and row counts. Then:

1. Start the backend against `vyra_restore_check` (`ddl-auto=validate`): it must start without schema errors.
2. Log in to the admin portal against it and open a member: balance decrypts, history loads.
3. Record the date, dump file, row counts and result in the ops log; drop the scratch database.

## 4. Real restore (incident)

1. Stop the backend (no writes during the restore).
2. Take a backup of the current state, even if broken.
3. Restore the chosen dump into a new database, verify it as in section 3.
4. Point `DATABASE_URL` to the restored database and start the backend; Flyway applies any newer migration.
5. Tell members about data written after the dump time, if any was lost.
