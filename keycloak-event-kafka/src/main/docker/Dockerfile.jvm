FROM eclipse-temurin:21-jre

ENV LANGUAGE='en_US:en'

# Configure the JAVA_OPTIONS, you can add -XshowSettings:vm to also display the heap size.
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# Install dependencies
USER root
RUN apt-get update \
    && apt-get install -y curl ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create deployments directory
RUN mkdir -p /deployments/lib /deployments/app /deployments/quarkus \
    && chown -R 1001 /deployments \
    && chmod -R "g+rwX" /deployments \
    && chown -R 1001:root /deployments

# Copy the quarkus-run.jar which was built in a previous step
COPY --chown=1001:root target/quarkus-app/lib/ /deployments/lib/
COPY --chown=1001:root target/quarkus-app/*.jar /deployments/
COPY --chown=1001:root target/quarkus-app/app/ /deployments/app/
COPY --chown=1001:root target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8082
USER 1001

WORKDIR /deployments
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"] 