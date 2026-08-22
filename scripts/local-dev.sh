#!/usr/bin/env bash
# ==============================================================================
# ALIGNER 백엔드 로컬 개발 환경 1초 원클릭 셋업 스크립트 (CISO Hardened)
# 
# 기능:
# 1. ALIGNER-PLATFORM/.runtime/secrets.sandbox.env 기반으로 localhost:5432 직결용 .env 동적 생성
# 2. 파일 권한 600 (소유자 전용 읽기/쓰기) 강제 부여로 로컬 권한 상승 및 유출 방지
# 3. kubectl port-forward로 가비아 DB(10.43.2.98:5432)를 로컬 localhost:5432로 터널링
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PLATFORM_RUNTIME="${PROJECT_ROOT}/../ALIGNER-PLATFORM/.runtime"

echo "================================================================="
echo "  🚀 [ALIGNER] 로컬 개발 환경 원클릭 셋업을 시작합니다."
echo "================================================================="

# 1. Kubeconfig 자동 확인 및 등록 (chmod 600 권한 제한)
if ! command -v kubectl &>/dev/null; then
    echo "❌ [오류] kubectl 명령어를 찾을 수 없습니다. 맥북에 kubectl을 설치해주세요."
    exit 1
fi

if [[ ! -f "${HOME}/.kube/config" && -z "${KUBECONFIG:-}" ]]; then
    if [[ -f "${PLATFORM_RUNTIME}/kubeconfig" ]]; then
        mkdir -p "${HOME}/.kube"
        cp "${PLATFORM_RUNTIME}/kubeconfig" "${HOME}/.kube/config"
        chmod 600 "${HOME}/.kube/config"
        echo "✅ [완료] ALIGNER-PLATFORM/.runtime/kubeconfig 를 ~/.kube/config 로 안전하게 등록했습니다 (권한 600)."
    else
        echo "⚠️ [경고] ~/.kube/config 파일이 없습니다. Tailscale과 kubeconfig 연결을 확인해주세요."
    fi
fi

# 2. 로컬 개발용 .env 동적 생성 (secrets.sandbox.env 기반 + chmod 600)
if [[ -f "${PLATFORM_RUNTIME}/secrets.sandbox.env" ]]; then
    sed -E 's|aligner-db-rw\.aligner-data\.svc:5432|localhost:5432|g; s|aligner-db-ro\.aligner-data\.svc:5432|localhost:5432|g' \
        "${PLATFORM_RUNTIME}/secrets.sandbox.env" > "${PROJECT_ROOT}/.env"
    chmod 600 "${PROJECT_ROOT}/.env"
    echo "✅ [완료] .runtime/secrets.sandbox.env 기반으로 ALIGNER-SERVER/.env 생성 완료 (권한 600)"
elif [[ -f "${PROJECT_ROOT}/.env" ]]; then
    chmod 600 "${PROJECT_ROOT}/.env"
    echo "ℹ️ [안내] 기존 ALIGNER-SERVER/.env 파일을 그대로 사용합니다 (권한 600 유지)."
else
    echo "⚠️ [경고] .runtime/secrets.sandbox.env 파일을 찾을 수 없습니다. .env를 확인해주세요."
fi

# 3. application-secret.properties 동기화 (chmod 600)
if [[ -f "${PROJECT_ROOT}/.env" ]]; then
    cp "${PROJECT_ROOT}/.env" "${PROJECT_ROOT}/application-secret.properties"
    chmod 600 "${PROJECT_ROOT}/application-secret.properties"
    echo "✅ [완료] ALIGNER-SERVER/application-secret.properties 동기화 완료 (권한 600)"
fi

# 4. 가비아 원격 DB 포트포워딩 실행 (exec로 시그널 핸들링 & 포트 릴리즈)
echo "-----------------------------------------------------------------"
echo "🐘 가비아 개발 DB (aligner-db-rw:5432) -> localhost:5432 터널링 시작"
echo "👉 이 터미널을 켜둔 채로 인텔리제이에서 Spring Boot를 실행하세요!"
echo "   (종료하려면 Ctrl + C 를 누르세요)"
echo "-----------------------------------------------------------------"

exec kubectl port-forward -n aligner-data svc/aligner-db-rw 5432:5432
