#!/usr/bin/env bash

set -u

ports=(1883 9001 8080 8081 8082 8083 8090 8091)

if command -v lsof >/dev/null 2>&1; then
    for port in "${ports[@]}"; do
        pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)

        if [[ -n "$pids" ]]; then
            echo "Terminando procesos del puerto $port: $pids"
            kill $pids
        else
            echo "Puerto $port libre"
        fi
    done
elif command -v fuser >/dev/null 2>&1; then
    for port in "${ports[@]}"; do
        echo "Terminando procesos del puerto $port"
        fuser -k "$port/tcp" 2>/dev/null || echo "Puerto $port libre"
    done
else
    echo "Error: se necesita lsof o fuser para liberar los puertos." >&2
    exit 1
fi
