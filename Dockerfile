FROM openjdk:11-jre-slim
COPY target/streams-deduplicator-1.0-SNAPSHOT.jar /opt/app.jar
EXPOSE 8080
CMD ["java", "-jar", "/opt/app.jar"]
