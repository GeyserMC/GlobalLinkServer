FROM eclipse-temurin:21-jre

WORKDIR /app
COPY build/libs/GlobalLinkServer.jar GlobalLinkServer.jar

CMD ["java", "-jar", "GlobalLinkServer.jar"]
