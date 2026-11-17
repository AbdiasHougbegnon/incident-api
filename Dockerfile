FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

COPY src ./src
RUN bash -c 'for i in 1 2 3 4 5; do ./gradlew --no-daemon bootJar -x test && exit 0; echo "Tentative $i echouee, nouvelle tentative dans 15s..."; sleep 15; done; exit 1'

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN useradd --system --uid 1001 --create-home appuser
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
