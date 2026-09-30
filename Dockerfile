# Multi-stage build. Pinned digests are set in CI (no :latest, SECURITY-10).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre AS runtime
# Non-root user (SECURITY-09)
RUN useradd -r -u 10001 appuser
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
