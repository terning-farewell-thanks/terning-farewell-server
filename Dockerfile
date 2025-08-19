FROM amazoncorretto:21

WORKDIR /app

COPY build/libs/farewell-server-0.0.1-SNAPSHOT.jar /app/app.jar

ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

CMD ["java", "-Duser.timezone=$TZ", "-jar", "-Dspring.profiles.active=$SPRING_PROFILES_ACTIVE", "app.jar"]
