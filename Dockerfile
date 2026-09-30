FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src/main src/main
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/target/printadmin-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
