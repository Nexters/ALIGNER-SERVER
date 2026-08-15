#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
application="$repo_root/application-api/src/main/resources/application.yml"
example="$repo_root/application-secret.properties.example"
dockerignore="$repo_root/.dockerignore"

expected=$(printf '%s\n' \
  DB_URL DB_USERNAME DB_PASSWORD SERVER_PORT JWT_SECRET \
  KAKAO_CLIENT_ID KAKAO_CLIENT_SECRET SPRINGDOC_ENABLED YMOVE_API_KEY | sort)
actual=$(sed -nE 's/^([A-Z][A-Z0-9_]*)=$/\1/p' "$example" | sort)

if [[ "$actual" != "$expected" ]]; then
  echo "application-secret.properties.example의 환경변수 계약이 다릅니다." >&2
  exit 1
fi

grep -Fq 'import: "optional:file:./application-secret.properties"' "$application"
grep -Fxq '**' "$dockerignore"
grep -Fxq '!application-api.jar' "$dockerignore"
for name in $expected; do
  grep -Fq "\${$name" "$application"
done
