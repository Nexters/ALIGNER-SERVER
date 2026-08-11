FROM eclipse-temurin:25-jre-noble

WORKDIR /app

RUN groupadd --system aligner \
    && useradd --system --gid aligner --home-dir /app --no-create-home aligner

COPY --chown=aligner:aligner application-api.jar application.jar

USER aligner

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
