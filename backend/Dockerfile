# =========================
# Stage 1: Build the backend
# =========================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

COPY src src

RUN ./gradlew clean build -x test


# =========================
# Stage 2: Get Liquibase CLI
# =========================
FROM liquibase/liquibase:5.0.2 AS liquibase-cli


# =========================
# Stage 3: Run the backend
# =========================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

COPY --from=liquibase-cli /liquibase /opt/liquibase

ENV PATH="/opt/liquibase:${PATH}"

EXPOSE 8085

CMD ["java", "-jar", "app.jar"]