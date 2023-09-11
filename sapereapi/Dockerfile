# Start with a base image containing Java runtime
# FROM openjdk:8-jdk-alpine
FROM openjdk:11.0.11-jre-slim


ARG REST_PORT=9090
ARG MAIN_PORT=10009
ARG NODE_CONFIG=FOO
ARG HOST=localhost
ARG NEIHBOURS=N0:localhost:10000:9090
ARG INIT_SCRIPT=init_script.sql
ARG CSV_FILE=
ARG URL_FORCASTING=
ARG MODE_AUTO=


# Add Maintainer Info
# LABEL maintainer="houcembenmahfoudh@gmail.com"
LABEL maintainer="philippe.glass@unige.ch"

ENV TZ="Europe/Zurich"

# Add a volume pointing to /log
VOLUME /log


ENV V_NODE_CONFIG=${NODE_CONFIG}
ENV V_REST_PORT=${REST_PORT}
ENV V_MAIN_PORT=${MAIN_PORT}
ENV V_HOST=${HOST}
ENV V_NEIHBOURS=${NEIHBOURS}
ENV V_INIT_SCRIPT=${INIT_SCRIPT}
ENV V_CSV_FILE=${CSV_FILE}
ENV V_URL_FORCASTING=${URL_FORCASTING}
ENV V_MODE_AUTO=${MODE_AUTO}



# Make REST and MAIN port available to the world outside this container
EXPOSE ${V_REST_PORT}
EXPOSE ${V_MAIN_PORT}

# The application's jar file
# ARG JAR_FILE=target/sapereapi-0.0.1-SNAPSHOT.jar
ARG JAR_FILE=coordination_platform.jar


# Add the application's jar to the container
# ADD ${JAR_FILE} sapereapi.jar

ADD ${JAR_FILE} coordination_platform.jar
# ADD ${CSV_FILE} ${CSV_FILE}

# Run the jar file 
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/sapereapi.jar"]
#ENTRYPOINT ["java", "-jar", "coordination_platform.jar", "node1sqlite", "init_script:init_script.sql"]
#ENTRYPOINT ["java", "-jar", "coordination_platform.jar", "${TEST2}", "init_script:init_script.sql"]

RUN echo V_MODE_AUTO=${V_MODE_AUTO} V_CSV_FILE=${V_CSV_FILE}
#RUN ls ${CSV_FILE}


ENTRYPOINT java -jar coordination_platform.jar ${V_NODE_CONFIG} -init_script:${V_INIT_SCRIPT} -lsa.server.neighbours:${V_NEIHBOURS} -lsa_server.host:${V_HOST} -mode_auto:${V_MODE_AUTO} -csv_file:${V_CSV_FILE} -url_forcasting:${V_URL_FORCASTING}
