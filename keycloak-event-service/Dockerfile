# Stage 1: Build
FROM maven:3.9.5-eclipse-temurin-21 as build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM registry.access.redhat.com/ubi8/openjdk-21-runtime:1.18
WORKDIR /work/
COPY --from=build /app/target/*-runner.jar /work/application.jar
COPY --from=build /app/target/lib /work/lib

# Set up permissions for user `1001`
RUN chmod 775 /work /work/lib /work/application.jar \
    && chown -R 1001 /work \
    && chmod -R "g+rwX" /work \
    && chown -R 1001:root /work

EXPOSE 8080
EXPOSE 9000

USER 1001

CMD ["java", "-jar", "application.jar"] 