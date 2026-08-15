#!/usr/bin/env bash
#
# 프론트 연동 테스트용 JWT 를 만든다. 카카오 로그인 없이 API 를 부를 수 있게 하는 용도다.
#
#   JWT_SECRET=<시크릿> ./scripts/dev-token.sh            # 임시 회원(900001) 토큰
#   JWT_SECRET=<시크릿> ./scripts/dev-token.sh 12         # 특정 회원 토큰
#
# **토큰을 저장소에 커밋하지 않는다.** 만료가 14 일이라 금방 죽고, 커밋하면 그 순간
# 시크릿으로 서명된 값이 히스토리에 남는다. 필요할 때마다 이 스크립트로 다시 만든다.
#
# 서버가 검증하는 것은 서명(HS256)·exp·sub 셋뿐이다. iss 는 발급만 하고 검증하지 않는다
# (support-web/auth/NimbusJwtTokenProvider.kt).
#
# 이 토큰이 통하려면 그 member_id 가 DB 에 있어야 한다. 임시 회원 900001 은
# member/schema seed 가 **LIQUIBASE_CONTEXTS=dev 일 때만** 넣는다.
set -euo pipefail

MEMBER_ID="${1:-900001}"
ISSUER="${JWT_ISSUER:-aligner}"
EXPIRES_IN="${JWT_EXPIRATION_SECONDS:-1209600}"   # 14 일. application.yml 기본값과 같다

if [[ -z "${JWT_SECRET:-}" ]]; then
  echo "JWT_SECRET 이 필요합니다. 서버의 aligner.auth.jwt.secret 과 같은 값이어야 합니다." >&2
  echo "  예: JWT_SECRET=\"\$(grep JWT_SECRET .env | cut -d= -f2-)\" $0" >&2
  exit 1
fi

# HS256 은 32 바이트 이상을 요구한다. 서버도 기동 시점에 같은 검사를 한다.
if (( ${#JWT_SECRET} < 32 )); then
  echo "JWT_SECRET 이 ${#JWT_SECRET} 바이트입니다. HS256 이라 32 바이트 이상이어야 합니다." >&2
  exit 1
fi

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

IAT=$(date +%s)
EXP=$((IAT + EXPIRES_IN))

HEADER=$(printf '{"alg":"HS256","typ":"JWT"}' | b64url)
PAYLOAD=$(printf '{"iss":"%s","sub":"%s","iat":%s,"exp":%s}' "$ISSUER" "$MEMBER_ID" "$IAT" "$EXP" | b64url)
SIGNATURE=$(printf '%s.%s' "$HEADER" "$PAYLOAD" \
  | openssl dgst -sha256 -mac HMAC -macopt "key:$JWT_SECRET" -binary \
  | b64url)

printf '%s.%s.%s\n' "$HEADER" "$PAYLOAD" "$SIGNATURE"

echo >&2
echo "member_id : $MEMBER_ID" >&2
echo "만료      : $(date -r "$EXP" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -d "@$EXP" '+%Y-%m-%d %H:%M:%S')" >&2
echo >&2
echo "사용 예:" >&2
echo "  curl -H \"Authorization: Bearer \$TOKEN\" http://localhost:8080/members/me" >&2
