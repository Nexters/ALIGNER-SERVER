#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
application="$repo_root/application-api/src/main/resources/application.yml"
dockerignore="$repo_root/.dockerignore"

expected=$(printf '%s\n' \
  DB_URL DB_USERNAME DB_PASSWORD SERVER_PORT JWT_SECRET \
  KAKAO_CLIENT_ID KAKAO_CLIENT_SECRET SPRINGDOC_ENABLED YMOVE_API_KEY | sort)

grep -Fxq '**' "$dockerignore"
grep -Fxq '!application-api.jar' "$dockerignore"
for name in $expected; do
  grep -Fq "\${$name" "$application"
done
echo "✓ 런타임 환경변수 9개 계약 검증 완료."
