FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app


COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw package -DskipTests


CMD ["sh", "-c", "java -jar target/*.jar"]

EXPOSE 8080