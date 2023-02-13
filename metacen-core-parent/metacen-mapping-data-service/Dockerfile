FROM adoptopenjdk/openjdk11:alpine
VOLUME /logs
VOLUME /config
EXPOSE 9606
COPY metacen-mapping-data-service-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]