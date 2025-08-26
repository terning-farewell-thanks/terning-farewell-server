FROM amazoncorretto:21

WORKDIR /app

COPY build/libs/farewell-server-0.0.1-SNAPSHOT.jar /app/app.jar

ENV TZ=Asia/Seoul

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
