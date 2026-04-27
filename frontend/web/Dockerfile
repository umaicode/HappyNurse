# ─────────────────────────────────────────────
# Stage 1: Dependencies (pnpm install)
# ─────────────────────────────────────────────
FROM node:22-alpine AS deps

WORKDIR /app

# pnpm 활성화 (Node 16.13+에 내장된 corepack 사용)
RUN corepack enable && corepack prepare pnpm@latest --activate

# 의존성 파일만 먼저 복사 → 이 레이어는 의존성 변경 없으면 캐시
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# ─────────────────────────────────────────────
# Stage 2: Build (next build)
# ─────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

RUN corepack enable && corepack prepare pnpm@latest --activate

# 의존성 복사
COPY --from=deps /app/node_modules ./node_modules
# 소스 복사
COPY . .

# basePath 환경별 분리 (dev=/dev, prod=빈값)
# Jenkinsfile에서 --build-arg NEXT_PUBLIC_BASE_PATH 전달
ARG NEXT_PUBLIC_BASE_PATH=
ENV NEXT_PUBLIC_BASE_PATH=$NEXT_PUBLIC_BASE_PATH

# Next.js 빌드
# standalone 모드로 빌드해서 runtime 이미지를 최소화
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

# ─────────────────────────────────────────────
# Stage 3: Runtime (Next standalone)
# ─────────────────────────────────────────────
FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# non-root 유저
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Next.js standalone 산출물만 복사 (이미지 크기 ~50MB로 극감)
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/public ./public

USER nextjs

EXPOSE 3000

CMD ["node", "server.js"]