#!/bin/sh
set -e

vault kv put secret/incident-api \
  db-password="$DB_PASSWORD" \
  jwt-secret="$JWT_SECRET" \
  admin-email="$ADMIN_EMAIL" \
  admin-password="$ADMIN_PASSWORD"

echo "Secrets ecrits dans Vault (secret/incident-api)."
