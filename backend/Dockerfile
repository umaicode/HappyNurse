# ─────────────────────────────────────────────
# Stage 1: Build (Gradle)
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# 의존성 캐싱 최적화:
# gradle wrapper와 설정 파일만 먼저 복사 → 변경 없으면 dependencies 캐시 유지
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Gradle Wrapper 실행 권한
RUN chmod +x gradlew

# 의존성 다운로드 (소스 변경돼도 이 레이어는 캐시됨)
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ─────────────────────────────────────────────
# Stage 2: Runtime (JRE only)
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# non-root user로 실행 (보안)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# 빌드 단계에서 생성된 jar만 복사
COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

# JVM 옵션 (컨테이너 환경 최적화)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# 헬스체크용 actuator가 있다면 /actuator/health, 없으면 그냥 curl 제거 가능
# 아직 actuator 의존성 안 넣었으니 헬스체크는 Step 7-B에서 docker-compose로
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]