#!/bin/bash

mvn install:install-file -Dfile=sapereV1.jar -DgroupId=com.sapere -DartifactId=sapere -Dversion=1.0 -Dpackaging=jar

mvn clean package

docker build -t faysalsaber/springbootapi:1 .
docker push faysalsaber/springbootapi:1

