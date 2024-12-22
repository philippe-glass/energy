#!/bin/bash

mvn install:install-file -Dfile=sapereV1.jar -DgroupId=com.sapere -DartifactId=sapere -Dversion=1.0 -Dpackaging=jar

mvn clean package

source /etc/profile.d/ant.sh
ant -version
ant

# docker build -t faysalsaber/springbootapi:1 .
# docker push faysalsaber/springbootapi:1

# docker build -t philippeglass1/light_api:1 .

export NODE1=N1:node1:10001:9191
export NODE2=N2:node2:10002:9292
export NODE3=N3:node3:10003:9393
export NODE4=N4:node4:10004:9494

# Neighbours of Node1, Node2, Node3 etc ...
export NB_NODE1="$NODE2"
export NB_NODE2="$NODE1,$NODE3"
export NB_NODE3="$NODE2,$NODE4"
export NB_NODE4="$NODE3"

echo NB_NODE3=$NB_NODE3

docker build -t philippeglass1/coordination_platform:1 --build-arg REST_PORT=9191 --build-arg MAIN_PORT=10001 --build-arg NODE_CONFIG=node1sqlite --build-arg HOST=node1 --build-arg NEIHBOURS= --build-arg MODE_AUTO=1 .

# docker build -t philippeglass1/coordination_platform_n1:1 --build-arg REST_PORT=9191 --build-arg MAIN_PORT=10001 --build-arg NODE_CONFIG=node1sqlite --build-arg HOST=node1 --build-arg NEIHBOURS=$NB_NODE1 --build-arg MODE_AUTO=1 .
# docker build -t philippeglass1/coordination_platform_n2:1 --build-arg REST_PORT=9292 --build-arg MAIN_PORT=10002 --build-arg NODE_CONFIG=node2sqlite --build-arg HOST=node2 --build-arg NEIHBOURS=$NB_NODE2 --build-arg MODE_AUTO=1 --network=host .
# docker build -t philippeglass1/coordination_platform_n3:1 --build-arg REST_PORT=9393 --build-arg MAIN_PORT=10003 --build-arg NODE_CONFIG=node3sqlite --build-arg HOST=node3 --build-arg NEIHBOURS=$NB_NODE3 --build-arg MODE_AUTO=1 --network=host .
# docker build -t philippeglass1/coordination_platform_n4:1 --build-arg REST_PORT=9494 --build-arg MAIN_PORT=10004 --build-arg NODE_CONFIG=node4sqlite --build-arg HOST=node4 --build-arg NEIHBOURS=$NB_NODE4 --build-arg MODE_AUTO=1 --network=host .




cat /proc/version

isInFile=$(cat /proc/version | grep -c "Debian")
echo isInFile=$isInFile
if [[ $isInFile -eq 1 ]]
then
        echo "debian found"
        docker push philippeglass1/coordination_platform:1
else
        echo "debian not found"
fi


# docker push philippeglass1/coordination_platform_n1:1
# docker push philippeglass1/coordination_platform_n2:1
# docker push philippeglass1/coordination_platform_n3:1
# docker push philippeglass1/coordination_platform_n4:1

