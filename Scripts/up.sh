#!/bin/bash
docker compose --env-file ./.env.dev -f ./docker/docker-compose.yml up -d --build