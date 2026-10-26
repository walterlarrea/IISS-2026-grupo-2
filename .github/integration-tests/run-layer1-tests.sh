#!/usr/bin/env bash
set -euo pipefail

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: Docker is not running. Start Docker and try again."
  echo
  echo "========================================"
  echo "❌ Layer 1 integration tests FAILED"
  echo "========================================"
  exit 1
fi

cd "$(dirname "$0")/../.."
set +e
mvn -pl controlador -am -DskipTests install
test_exit=$?
if [ "$test_exit" -eq 0 ]; then
  mvn -pl controlador failsafe:integration-test failsafe:verify
  test_exit=$?
fi
set -e

echo
echo "========================================"
if [ "$test_exit" -eq 0 ]; then
  echo "✅ Layer 1 integration tests PASSED"
else
  echo "❌ Layer 1 integration tests FAILED"
fi
echo "========================================"
exit "$test_exit"
