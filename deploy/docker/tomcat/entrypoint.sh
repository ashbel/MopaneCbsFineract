#!/bin/bash
set -euo pipefail

export MYSQL_HOST="${MYSQL_HOST:-mysql}"
export MYSQL_PORT="${MYSQL_PORT:-3306}"
export MYSQL_USER="${MYSQL_USER:-mifos}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"

envsubst '${MYSQL_HOST} ${MYSQL_PORT} ${MYSQL_USER} ${MYSQL_PASSWORD}' \
  < /usr/local/tomcat/conf/server.xml.template \
  > /usr/local/tomcat/conf/server.xml

touch /usr/local/tomcat/logs/fineract-platform.log

# Old Fineract seeds tenant_server_connections with host=localhost.
# Forward container-local 3306 to the MySQL service so those rows work unchanged.
if ! (echo >/dev/tcp/127.0.0.1/3306) >/dev/null 2>&1; then
  echo "Starting socat localhost:3306 -> ${MYSQL_HOST}:${MYSQL_PORT}"
  socat TCP-LISTEN:3306,fork,reuseaddr TCP:${MYSQL_HOST}:${MYSQL_PORT} &
  sleep 1
fi

echo "Starting Tomcat with JNDI -> ${MYSQL_HOST}:${MYSQL_PORT} as ${MYSQL_USER}"
exec catalina.sh run
