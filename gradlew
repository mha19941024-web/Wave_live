#!/bin/sh
set -e
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -x "$APP_HOME/gradle-8.9/bin/gradle" ]; then
  exec "$APP_HOME/gradle-8.9/bin/gradle" "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle is not installed. Use GitHub Actions workflow or install Gradle 8.9." >&2
exit 1
