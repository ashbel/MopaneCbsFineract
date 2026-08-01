#!/bin/bash
set -euo pipefail

echo "Waiting for mifosplatform-tenants.tenant_server_connections..."
for i in $(seq 1 90); do
  if mysql -h"$MYSQL_HOST" -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e \
    "SELECT 1 FROM \`mifosplatform-tenants\`.tenant_server_connections LIMIT 1" >/dev/null 2>&1; then
    break
  fi
  sleep 10
done

mysql -h"$MYSQL_HOST" -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
USE \`mifosplatform-tenants\`;
UPDATE tenant_server_connections
SET
  schema_server = 'mysql',
  schema_server_port = '3306',
  schema_username = 'root',
  schema_password = '${MYSQL_ROOT_PASSWORD}';
SELECT id, schema_server, schema_server_port, schema_name, schema_username FROM tenant_server_connections;
SQL
echo "Tenant server connections updated to host=mysql"
