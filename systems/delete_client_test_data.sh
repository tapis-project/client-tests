#!/bin/sh
# Use psql to remove client test artifacts from the DB
# 
#   against the systems service running locally and the other
#   services (tenants, sk, auth, tokens) running in DEV, STAGING, or PROD
# NOTE: For safety, for now do not allow running against PROD
# Use docker to start up the systems service locally using an image
#   Image used is based on TAPIS_RUN_ENV
# Use mvn to run the systems-client integration tests.
#
#
# Following env variables must be set:
#     TAPIS_DB_PASSWORD
#     TAPIS_DB_JDBC_URL
#
# To run the client integration tests the following env variables must be set:
#   TAPIS_FILES_SVC_PASSWORD - used for testing credential retrieval

if [ -z "$TAPIS_DB_PASSWORD" ] || [ -z "$TAPIS_DB_JDBC_URL" ]; then
  echo "Please set env variable TAPIS_DB_PASSWORD and TAPIS_DB_JDBC_URL"
  exit 1
fi

DB_NAME=$(echo "$TAPIS_DB_JDBC_URL" | awk -F"/" '{print $4}')
DB_HOST=$(echo "$TAPIS_DB_JDBC_URL" | awk -F"/" '{print $3}' | awk -F":" '{print $1}')
DB_PORT=$(echo "$TAPIS_DB_JDBC_URL" | awk -F"/" '{print $3}' | awk -F":" '{print $2}')

echo "Using DB Host:Port:Name = $DB_HOST:$DB_PORT:$DB_NAME"

docker run -i --rm --network="host" bitnami/postgresql:latest /bin/bash << EOF
PGPASSWORD=${TAPIS_DB_PASSWORD} psql --host=${DB_HOST} --port=${DB_PORT} --username=tapis --dbname=${DB_NAME} -q -P pager << EOB
DELETE FROM tapis_sys.systems WHERE NAME LIKE 'CSys\_Clt%'
EOB
EOF
