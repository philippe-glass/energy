#Deriving the latest base image
FROM python:latest

RUN pip install requests

#Labels as key value pair
LABEL maintainer="philippe.glass@unige.ch"

ARG HOST=localhost


# Any working directory can be chosen as per choice like '/' or '/home' etc
# i have chosen /usr/app/src
WORKDIR /usr/app/src

#to COPY the remote file at working directory in container
COPY test_coordination_platform.py ./
# Now the structure looks like this '/usr/app/src/test_coordination_platform.py'


ENV V_HOST=${HOST}
ENV V_TIMESTAMP='2021-10-27 21:15:00+0000'
ENV V_SAMPLING_NB=13

#CMD instruction should be used to run the software
#contained by your image, along with any arguments.

# CMD [ "python", "./test_coordination_platform.py",  "${V_HOST}"]
ENTRYPOINT python ./test_coordination_platform.py ${V_HOST} ${V_TIMESTAMP} ${V_SAMPLING_NB}
