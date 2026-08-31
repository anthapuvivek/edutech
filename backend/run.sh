#!/usr/bin/env bash
<<<<<<< HEAD
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
=======
# Starts the LearntriX backend with backend/.env loaded into the environment.
# Spring Boot does not read .env files on its own; without this the DB password
# and SMTP credentials never reach the application.
set -euo pipefail
cd "$(dirname "$0")"

if [ -f .env ]; then
  # Parse .env as data rather than sourcing it. Sourcing runs the file as shell, so any
  # unquoted value containing a space (e.g. a display name) is treated as a command and
  # aborts the script under `set -e` — before the DB password is ever exported.
  while IFS= read -r line || [ -n "$line" ]; do
    line=${line%$'\r'}
    case $line in ''|'#'*) continue ;; esac
    case $line in *=*) ;; *) continue ;; esac
    key=${line%%=*}
    value=${line#*=}
    # Trim surrounding whitespace from the key; values are taken verbatim.
    key=$(printf '%s' "$key" | tr -d '[:space:]')
    case $key in ''|*[!A-Za-z0-9_]*) continue ;; esac
    # Strip one layer of matching quotes so quoted and bare values behave the same.
    case $value in
      \"*\") value=${value#\"}; value=${value%\"} ;;
      \'*\') value=${value#\'}; value=${value%\'} ;;
    esac
    export "$key=$value"
  done < .env
  echo "Loaded environment from $(pwd)/.env"
else
  echo "No .env found - falling back to application.yml defaults."
fi

if [ -z "${DB_PASSWORD:-}" ]; then
  echo "ERROR: DB_PASSWORD is not set. application.yml would fall back to its placeholder" >&2
  echo "       default and Postgres would reject it with 'password authentication failed" >&2
  echo "       for user \"postgres\"'. Set DB_PASSWORD in backend/.env." >&2
  exit 1
fi

if [ -z "${MAIL_USERNAME:-}" ] || [ -z "${MAIL_PASSWORD:-}" ]; then
  echo "WARNING: MAIL_USERNAME / MAIL_PASSWORD are empty - activation emails will NOT be delivered."
fi

exec mvn spring-boot:run
>>>>>>> b72e728 (application updated)
