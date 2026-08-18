#!/usr/bin/env bash
# Nightly per-tenant MySQL dumps -> gzip -> Google Drive (rclone).
# Retention: keep 7 days on Google Drive (and locally).
set -euo pipefail

FINERACT_DIR="${FINERACT_DIR:-/opt/fineract}"
ENV_FILE="${ENV_FILE:-$FINERACT_DIR/.env}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-fineract-legacy-mysql}"
RCLONE_REMOTE="${RCLONE_REMOTE:-gdrive}"
GDRIVE_FOLDER="${GDRIVE_FOLDER:-MopaneCbs/FineractBackups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
LOCAL_ROOT="${LOCAL_ROOT:-/var/backups/fineract}"
# Africa/Harare date stamp even if host TZ is Europe/Berlin
STAMP="${STAMP:-$(TZ=Africa/Harare date +%F)}"
LOCK_FILE="${LOCK_FILE:-/var/lock/fineract-gdrive-backup.lock}"
TEXTFILE_DIR="${TEXTFILE_DIR:-/var/lib/node_exporter/textfile}"
TEXTFILE_PROM="${TEXTFILE_PROM:-$TEXTFILE_DIR/fineract_backup.prom}"
BACKUP_START_EPOCH="$(date +%s)"

log() { echo "[$(TZ=Africa/Harare date '+%F %T %Z')] $*"; }

write_backup_metrics() {
  local success="$1"
  local now duration
  now="$(date +%s)"
  duration=$((now - BACKUP_START_EPOCH))
  mkdir -p "$TEXTFILE_DIR"
  {
    echo "# HELP fineract_backup_success 1 if last backup run succeeded"
    echo "# TYPE fineract_backup_success gauge"
    echo "fineract_backup_success ${success}"
    echo "# HELP fineract_backup_last_success_unixtime Unix time of last successful backup"
    echo "# TYPE fineract_backup_last_success_unixtime gauge"
    if [[ "$success" -eq 1 ]]; then
      echo "fineract_backup_last_success_unixtime ${now}"
    elif [[ -f "$TEXTFILE_PROM" ]]; then
      # Preserve prior success timestamp on failure
      awk '/^fineract_backup_last_success_unixtime /{print}' "$TEXTFILE_PROM" \
        || echo "fineract_backup_last_success_unixtime 0"
    else
      echo "fineract_backup_last_success_unixtime 0"
    fi
    echo "# HELP fineract_backup_duration_seconds Duration of last backup attempt"
    echo "# TYPE fineract_backup_duration_seconds gauge"
    echo "fineract_backup_duration_seconds ${duration}"
  } >"${TEXTFILE_PROM}.tmp"
  mv "${TEXTFILE_PROM}.tmp" "$TEXTFILE_PROM"
}

trap 'ec=$?; if [[ $ec -ne 0 ]]; then write_backup_metrics 0 || true; fi' EXIT

if [[ ! -f "$ENV_FILE" ]]; then
  log "ERROR: missing $ENV_FILE"
  exit 1
fi
# shellcheck disable=SC1090
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD missing in .env}"

if ! command -v rclone >/dev/null 2>&1; then
  log "ERROR: rclone not installed"
  exit 1
fi

if ! docker inspect "$MYSQL_CONTAINER" >/dev/null 2>&1; then
  log "ERROR: MySQL container $MYSQL_CONTAINER not found"
  exit 1
fi

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  log "ERROR: another backup is already running"
  exit 1
fi

LOCAL_DIR="$LOCAL_ROOT/$STAMP"
mkdir -p "$LOCAL_DIR"
REMOTE_PATH="${RCLONE_REMOTE}:${GDRIVE_FOLDER}/${STAMP}"

log "Starting backup stamp=$STAMP remote=$REMOTE_PATH"

mysql_exec() {
  docker exec "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --batch --raw "$@"
}

mysqldump_db() {
  local db="$1"
  docker exec "$MYSQL_CONTAINER" mysqldump \
    -uroot -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction \
    --routines \
    --triggers \
    --databases "$db"
}

# Tenant schemas from registry + the tenants DB itself
mapfile -t DBS < <(mysql_exec -N -e "
  SELECT schema_name FROM \`mifosplatform-tenants\`.tenant_server_connections
  WHERE schema_name IS NOT NULL AND schema_name <> ''
  ORDER BY schema_name;
  SELECT 'mifosplatform-tenants';
" | awk 'NF' | sort -u)

if [[ ${#DBS[@]} -eq 0 ]]; then
  log "ERROR: no databases discovered"
  exit 1
fi

log "Databases: ${DBS[*]}"

failed=0
for db in "${DBS[@]}"; do
  out="$LOCAL_DIR/${db}.sql.gz"
  log "Dumping $db -> $out"
  if mysqldump_db "$db" | gzip -1 >"$out"; then
    ls -lh "$out" | awk '{print "[size]", $5, $9}'
  else
    log "ERROR: dump failed for $db"
    rm -f "$out"
    failed=1
  fi
done

if [[ "$failed" -ne 0 ]]; then
  log "ERROR: one or more dumps failed; skipping upload"
  write_backup_metrics 0
  exit 1
fi

log "Uploading to $REMOTE_PATH"
rclone copy "$LOCAL_DIR" "$REMOTE_PATH" --checksum --retries 3 --low-level-retries 10

log "Pruning Google Drive backups older than ${RETENTION_DAYS} days"
# Delete aged files only; then drop empty date folders (ignore non-empty).
rclone delete "${RCLONE_REMOTE}:${GDRIVE_FOLDER}" --min-age "${RETENTION_DAYS}d" || true
rclone rmdirs "${RCLONE_REMOTE}:${GDRIVE_FOLDER}" --leave-root 2>/dev/null || true

log "Pruning local backups older than ${RETENTION_DAYS} days"
find "$LOCAL_ROOT" -mindepth 1 -maxdepth 1 -type d -mtime +"${RETENTION_DAYS}" -exec rm -rf {} +

log "Listing remote for $STAMP"
rclone ls "$REMOTE_PATH" || true

write_backup_metrics 1
trap - EXIT
log "Backup finished OK"
