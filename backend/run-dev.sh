#!/usr/bin/env bash
# Loads backend/.env and starts the application with the "dev" profile.
set -euo pipefail

if [ ! -f .env ]; then
  echo "backend/.env not found. Copy .env.example to .env and fill it in." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

echo "Starting NER-SmartLogix-AI (dev profile) ..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev
