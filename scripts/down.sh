#!/bin/bash

# Get the absolute directory of this script
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

docker compose -f "$SCRIPT_DIR/../docker/docker-compose.yml" down