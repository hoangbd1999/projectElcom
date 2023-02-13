FROM adoptopenjdk/openjdk11:alpine
VOLUME /logs
VOLUME /config
EXPOSE 9613
COPY metacen-report-service-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]