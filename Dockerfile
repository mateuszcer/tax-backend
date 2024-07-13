FROM amazoncorretto:21

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} application.jar

CMD apt-get update -y

ENTRYPOINT ["java", "-jar", "/application.jar"]
