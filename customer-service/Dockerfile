FROM eclipse-temurin:24-jdk
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn clean install -DskipTests
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "target/customer-service-0.0.1-SNAPSHOT.jar"]
