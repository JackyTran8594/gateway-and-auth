### STAGE 1: PREPARING AND MAVEN BUILDING
FROM maven:3.8.5-openjdk-11 AS builders
COPY pom.xml /app/
COPY src /app/src
WORKDIR /app/
RUN mvn clean test
RUN mvn clean install


### STAGE 2: DEPLOY APP
FROM openjdk:11.0.14-jdk
WORKDIR /app/
COPY --from=builders /app/target/euruka-0.0.1.jar /app/
ENTRYPOINT ["java", "-jar","euruka-0.0.1.jar"]
EXPOSE 8052



