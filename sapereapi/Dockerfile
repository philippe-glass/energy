# Start with a base image containing Java runtime
FROM openjdk:8-jdk-alpine

# Add Maintainer Info
LABEL maintainer="houcembenmahfoudh@gmail.com"

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 9090 available to the world outside this container
EXPOSE 9090

# The application's jar file
ARG JAR_FILE=target/sapereapi-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
ADD ${JAR_FILE} sapereapi.jar

# Run the jar file 
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/sapereapi.jar"]
