# Stage 1: Build dengan Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime yang ringan
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /target/*.jar app.jar

# Optimasi RAM untuk Free Tier Render (512MB)
ENV JAVA_OPTS="-Xmx300m -Xms300m"

EXPOSE 8000
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]