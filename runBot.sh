#!/bin/bash

if [ ! $1 ]; then
	echo "runBot <PROPERTIES FILE>"
	exit 1
fi

CLASSPATH=sonofcim.jar:commons-codec-1.3.jar:commons-dbcp-1.2.2.jar:commons-httpclient-3.1.jar:commons-logging-1.1.jar:commons-pool-1.3.jar:joda-time-1.5.2.jar:mallet-deps.jar:mallet.jar:mysql-connector-java-5.0.4.jar:pircbot.jar:spring-2.0.8.jar

echo $CLASSPATH
nohup java -cp $CLASSPATH -jar sonofcim.jar $1 $2 >> bot.log &
