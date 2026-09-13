#!/bin/bash
set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ENVIRONMENT=${1:-dev}

if [ "$ENVIRONMENT" = "dev" ]; then
    ENV_FILE="$SCRIPT_DIR/../.env.dev"
elif [ "$ENVIRONMENT" = "local" ]; then
    ENV_FILE="$SCRIPT_DIR/../.env.local"
else
    echo "Invalid environment: $ENVIRONMENT"
    echo "Use: build.sh dev/local"
    exit 1
fi

docker compose --env-file "$ENV_FILE" -f "$SCRIPT_DIR/../docker/docker-compose.yml" build
echo "Build nicely done!"