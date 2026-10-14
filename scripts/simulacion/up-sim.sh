#!/bin/bash

# Get the absolute directory of this script
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

CONECTAR_CON_SIMULADOR=true docker compose --env-file "$SCRIPT_DIR/../.env.dev" -f "$SCRIPT_DIR/../docker/docker-compose.yml" up -d --build