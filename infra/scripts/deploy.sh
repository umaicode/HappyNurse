#!/bin/bash
# ──────────────────────────────────────────────────────────────
# Blue-Green 배포 스크립트
#
# 사용법: deploy.sh <env> <service>
#   env:     dev | prod
#   service: backend | ai | frontend
#
# 예시: deploy.sh dev backend
# ──────────────────────────────────────────────────────────────

set -euo pipefail

# ──────────────────────────────────────────────
# 인자 파싱
# ──────────────────────────────────────────────
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <env> <service>"
    echo "  env:     dev | prod"
    echo "  service: backend | ai | frontend"
    exit 1
fi

ENV="$1"
SERVICE="$2"

if [[ "$ENV" != "dev" && "$ENV" != "prod" ]]; then
    echo "[ERROR] env must be 'dev' or 'prod'"
    exit 1
fi

if [[ "$SERVICE" != "backend" && "$SERVICE" != "ai" && "$SERVICE" != "frontend" ]]; then
    echo "[ERROR] service must be 'backend', 'ai', or 'frontend'"
    exit 1
fi

# ──────────────────────────────────────────────
# 서비스별 포트 매핑 (컨테이너 내부 포트)
# ──────────────────────────────────────────────
case "$SERVICE" in
    backend)  PORT=8080 ;;
    ai)       PORT=8000 ;;
    frontend) PORT=3000 ;;
esac

# ──────────────────────────────────────────────
# 경로 설정
# ──────────────────────────────────────────────
INFRA_DIR="/home/deploy/infra"
COMPOSE_FILE="${INFRA_DIR}/docker-compose.${ENV}.yml"
UPSTREAM_FILE="${INFRA_DIR}/nginx/conf.d/upstream-${SERVICE}-${ENV}.conf"

# ──────────────────────────────────────────────
# 유틸
# ──────────────────────────────────────────────
log()   { echo "[$(TZ=Asia/Seoul date +'%H:%M:%S')] $*"; }
die()   { echo "[ERROR] $*" >&2; exit 1; }

# ──────────────────────────────────────────────
# 1. 현재 활성 색 파악
# ──────────────────────────────────────────────
log "▶ Deploying ${SERVICE}-${ENV}"

if [ ! -f "$UPSTREAM_FILE" ]; then
    die "Upstream file not found: $UPSTREAM_FILE"
fi

if grep -q "${SERVICE}-${ENV}-blue" "$UPSTREAM_FILE"; then
    CURRENT="blue"
    NEW="green"
elif grep -q "${SERVICE}-${ENV}-green" "$UPSTREAM_FILE"; then
    CURRENT="green"
    NEW="blue"
else
    # 첫 배포 — 그냥 blue로 시작
    log "   No active color detected. Starting with blue."
    CURRENT=""
    NEW="blue"
fi

log "   Current: ${CURRENT:-none}, New: $NEW"

NEW_CONTAINER="${SERVICE}-${ENV}-${NEW}"
OLD_CONTAINER=""
if [ -n "$CURRENT" ]; then
    OLD_CONTAINER="${SERVICE}-${ENV}-${CURRENT}"
fi

# ──────────────────────────────────────────────
# 2. 새 색 컨테이너 기동
# ──────────────────────────────────────────────
log "▶ Starting $NEW_CONTAINER"

# 기존에 떠있는 동일 이름 컨테이너가 있으면 제거 (재배포 대비)
if docker ps -a --format '{{.Names}}' | grep -q "^${NEW_CONTAINER}$"; then
    log "   Removing stale container $NEW_CONTAINER"
    docker rm -f "$NEW_CONTAINER" >/dev/null
fi

cd "$INFRA_DIR"
docker compose -f "$COMPOSE_FILE" up -d --no-deps "$NEW_CONTAINER"

# ──────────────────────────────────────────────
# 3. 헬스체크 (TCP 포트 열림 확인)
# ──────────────────────────────────────────────
log "▶ Health check on $NEW_CONTAINER (port $PORT)"

MAX_RETRIES=40
RETRY_INTERVAL=3

for i in $(seq 1 $MAX_RETRIES); do
    # 컨테이너가 아직 안 떠있으면 기다림
    if ! docker ps --format '{{.Names}}' | grep -q "^${NEW_CONTAINER}$"; then
        log "   [$i/$MAX_RETRIES] Container not running yet..."
        sleep $RETRY_INTERVAL
        continue
    fi

    # TCP 포트 열림 체크 (컨테이너 안에서)
    if docker exec nginx sh -c "nc -z ${NEW_CONTAINER} ${PORT}" 2>/dev/null; then
        log "Health check passed (via nginx)"
        break
    fi

    if [ $i -eq $MAX_RETRIES ]; then
        log "   ✗ Health check failed after $MAX_RETRIES attempts"
        log "▶ Rolling back..."
        docker logs --tail 50 "$NEW_CONTAINER" || true
        docker rm -f "$NEW_CONTAINER" || true
        die "Deployment failed — rolled back. Old container $OLD_CONTAINER remains active."
    fi

    log "   [$i/$MAX_RETRIES] Waiting..."
    sleep $RETRY_INTERVAL
done

# ──────────────────────────────────────────────
# 4. Nginx upstream 파일 교체 + reload
# ──────────────────────────────────────────────
log "▶ Switching Nginx upstream to $NEW"

# 변수 이름 (예: backend_dev_target)
TARGET_VAR="${SERVICE}_${ENV}_target"

cat > "$UPSTREAM_FILE" <<EOF
# 현재 활성 색: ${NEW}
# Last updated: $(TZ=Asia/Seoul date '+%Y-%m-%d %H:%M:%S KST')
map "" \$${TARGET_VAR} {
    default "${NEW_CONTAINER}:${PORT}";
}
EOF

# Nginx 설정 검증 후 reload
if ! docker exec nginx nginx -t 2>&1; then
    die "Nginx config test failed!"
fi

docker exec nginx nginx -s reload
log "   ✓ Nginx reloaded"

# ──────────────────────────────────────────────
# 5. 이전 색 컨테이너 정리 (있을 경우)
# ──────────────────────────────────────────────
if [ -n "$OLD_CONTAINER" ] && docker ps --format '{{.Names}}' | grep -q "^${OLD_CONTAINER}$"; then
    log "▶ Cleaning up $OLD_CONTAINER (graceful shutdown in 10s)"
    # 기존 연결이 완료되도록 잠깐 대기
    sleep 10
    docker rm -f "$OLD_CONTAINER" >/dev/null
    log "   ✓ Removed $OLD_CONTAINER"
fi

log "✅ Deployment complete: ${SERVICE}-${ENV} is now on $NEW"
