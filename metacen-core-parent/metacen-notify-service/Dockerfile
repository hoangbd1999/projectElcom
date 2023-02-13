FROM adoptopenjdk/openjdk11:alpine
VOLUME /logs
VOLUME /config
EXPOSE 9604
COPY metacen-notify-service-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]