# syntax=docker/dockerfile:1

# The backend as one image (BE-40): built with the Gradle wrapper on JDK 21, run on a JRE.
# Tests aren't run here; CI runs them, with the Postgres container they need.
#
#   docker build -t salon-backend salon-backend
#
# Without SPRING_PROFILES_ACTIVE it runs like a developer's machine, configured through environment
# variables (see docker-compose.yml). With SPRING_PROFILES_ACTIVE=prod it needs the SALON_*
# variables listed in src/main/resources/application-prod.yml.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
COPY src/main src/main
# The cache keeps Gradle and the dependencies between builds on the same machine.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootWar \
    && cp "$(ls build/libs/*.war | grep -v plain)" /src/salon.war

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --home-dir /app salon && mkdir /app && chown salon /app
# The prod profile writes its log file under /app/logs.
WORKDIR /app
COPY --from=build /src/salon.war /app/salon.war
USER salon
# 8081, as in development and under the prod profile.
ENV SERVER_PORT=8081
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" > /dev/null || exit 1
# Sized from the container's memory limit rather than the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/salon.war"]
