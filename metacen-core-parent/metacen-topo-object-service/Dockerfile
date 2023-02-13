FROM openjdk:8-jdk-alpine

RUN apk add --update ffmpeg

WORKDIR /opt/vsat/neo4j-service

ADD target/*.jar neo4j-1.0.0.jar

RUN addgroup -g 1420 vsat-group
RUN adduser -D -u 1420 vsat-user -G vsat-group
RUN chown -R vsat-user:vsat-group /opt/vsat/media-service
USER vsat-user

#ENTRYPOINT ["java", "-jar", "-Xmx2048m", "media-service.jar"]
