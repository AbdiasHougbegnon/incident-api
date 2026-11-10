#!/bin/sh
set -e

mkdir -p /backups

while true; do
  TIMESTAMP=$(date +%Y%m%d_%H%M%S)
  echo "Sauvegarde de incidentdb -> /backups/incidentdb_$TIMESTAMP.dump"
  pg_dump -h postgres -U "$DB_USERNAME" -d incidentdb -F c -f "/backups/incidentdb_$TIMESTAMP.dump"

  find /backups -name "*.dump" -mtime "+$RETENTION_DAYS" -delete

  sleep 86400
done
