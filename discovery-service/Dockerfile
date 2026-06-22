FROM eclipse-temurin:24-jdk
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn clean install -DskipTests
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "target/discovery-service-0.0.1-SNAPSHOT.jar"]
