FROM adoptopenjdk/openjdk11:alpine
VOLUME /logs
VOLUME /config
EXPOSE 9608
COPY metacen-data-process-config-service-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]