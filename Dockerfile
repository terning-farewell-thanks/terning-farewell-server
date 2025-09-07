FROM amazoncorretto:21

WORKDIR /app

ADD https://dtdg.co/latest-java-tracer /app/dd-java-agent.jar

COPY build/libs/farewell-server-0.0.1-SNAPSHOT.jar /app/app.jar

ENV TZ=Asia/Seoul

EXPOSE 8080

CMD ["sh", "-c", "java -javaagent:/app/dd-java-agent.jar -Ddd.logs.injection=true -Ddd.profiling.enabled=true -Ddd.service=${DD_SERVICE} -Ddd.env=${DD_ENV} -Ddd.agent.host=${DD_AGENT_HOST} -jar /app/app.jar --spring.profiles.active=prod"]

