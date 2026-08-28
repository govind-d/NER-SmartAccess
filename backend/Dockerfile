# ---- Build stage -------------------------------------------------------------
# The build happens inside the image so that a machine with no JDK can still
# produce a runnable artefact. Dependencies are resolved in their own layer, so
# editing a Java file does not re-download the internet.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage -----------------------------------------------------------
# A JRE, not a JDK: smaller image, no compiler in production.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Never run as root inside a container.
RUN addgroup -S ner && adduser -S ner -G ner
COPY --from=build /build/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R ner:ner /app
USER ner

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
