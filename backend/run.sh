#!/usr/bin/env bash
# LearntriX Backend Runner (Bash)
# Loads environment variables from backend/.env and starts Spring Boot

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
    echo "Loading environment variables from $ENV_FILE"
    set -a
    # shellcheck source=/dev/null
    source "$ENV_FILE"
    set +a
else
    echo "WARNING: .env file not found at $ENV_FILE. Using default configurations."
fi

mvn spring-boot:run
