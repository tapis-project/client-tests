#!/bin/sh
# Start up a service locally using a docker image
# Environment value must be passed in as first argument: dev, staging, prod
# Service name must be passed in as second argument: systems, apps
# NOTE: For now, for safety, never run against prod
# Optionally accept docker image reference for starting service locally. Default is tapis/<service_name>:<run_env>
# Following services from a running tapis3 are required: tenants, tokens, security-kernel
# Base URL for remote services is determined by environment value passed in.
# Typically service will be available at http://localhost:8080/v3/<service_name>
# Following environment variables must be set:
#   TAPIS_SERVICE_PASSWORD
#   TAPIS_DB_PASSWORD
#   TAPIS_DB_JDBC_URL
#
# Following environment variable may be set for PORT. Default is 8080
#   TAPIS_SERVICE_PORT

PrgName=$(basename "$0")

USAGE1="Usage: $PrgName { dev, staging } {systems, apps} [ <image_name> ]"

# Run docker image for a service
TAPIS_RUN_ENV=$1
TST_SVC=$2
TST_IMG=$3

##########################################################
# Check number of arguments.
##########################################################
if [ $# -lt 2 -o $# -gt 3 ]; then
  echo "ERROR: Incorrect number of arguments"
  echo $USAGE1
  exit 1
fi

# Make sure we have the service password, db password and db URL
if [ -z "$TAPIS_SERVICE_PASSWORD" ]; then
  echo "ERROR: Please set env variable TAPIS_SERVICE_PASSWORD to the service password"
  echo $USAGE1
  exit 1
fi
if [ -z "$TAPIS_DB_PASSWORD" ]; then
  echo "ERROR: Please set env variable TAPIS_DB_PASSWORD"
  echo $USAGE1
  exit 1
fi
if [ -z "$TAPIS_DB_JDBC_URL" ]; then
  echo "ERROR: Please set env variable TAPIS_DB_JDBC_URL"
  echo $USAGE1
  exit 1
fi

# Set base url for services we depend on (tenants, tokens, security-kernel)
if [ "$TAPIS_RUN_ENV" = "dev" ]; then
 BASE_URL="https://master.develop.tapis.io"
elif [ "$TAPIS_RUN_ENV" = "staging" ]; then
 BASE_URL="https://master.staging.tapis.io"
# elif [ "$TAPIS_RUN_ENV" = "prod" ]; then
#  BASE_URL="https://master.tapis.io"
else
  echo "ERROR: Invalid TAPIS_RUN_ENV = $TAPIS_RUN_ENV"
  echo $USAGE1
  exit 1
fi

# Make sure service is supported
if [ "$TST_SVC" != "systems" -a "$TST_SVC" != "apps" ]; then
  echo "ERROR: Invalid service name: $TST_SVC"
  echo $USAGE1
  exit 1
fi

# Determine absolute path to location from which we are running.
export RUN_DIR=$(pwd)
export PRG_RELPATH=$(dirname "$0")
cd "$PRG_RELPATH"/. || exit
export PRG_PATH=$(pwd)

# Default test image for running svc locally is tapis/$TST_SVC:$TAPIS_RUN_ENV
if [ -z "$TST_IMG" ]; then
  TST_IMG=tapis/${TST_SVC}:${TAPIS_RUN_ENV}
fi

# Running with network=host exposes ports directly. Only works for linux
docker run -e TAPIS_SERVICE_PASSWORD="${TAPIS_SERVICE_PASSWORD}" \
           -e TAPIS_SERVICE_PORT="${TAPIS_SERVICE_PORT}" \
           -e TAPIS_TENANT_SVC_BASEURL="${BASE_URL}" \
           -e TAPIS_DB_PASSWORD="${TAPIS_DB_PASSWORD}" \
           -e TAPIS_DB_JDBC_URL="${TAPIS_DB_JDBC_URL}" \
           -d --rm --network="host" "${TST_IMG}"
