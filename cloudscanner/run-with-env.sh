#!/usr/bin/env bash
# Load cloudscanner/.env (if present) and start Spring Boot.
set -euo pipefail
cd "$(dirname "$0")"

if [[ -f .env ]]; then
  set -a
  # shellcheck source=/dev/null
  source .env
  set +a
  echo "Loaded environment from .env"
else
  echo "No .env file found — using existing shell environment only."
  echo "Copy .env.example to .env and add AWS credentials."
fi

exec ./mvnw spring-boot:run "$@"
