#!/bin/sh

function die_error {
	echo $1
	exit 1
}

function download_dependency {
	file=`echo $1 | sed s/http:.*\\\///`
	echo "Downloading $1 to $file"
	curl "$1" > lib/$file || dir_error "Could not downlad $file from $1, try looking elsewhere for this jar"
}

which curl || die_error "curl (http://curl.haxx.se/) is required to run this script"

echo "***************************************************************"
echo "*                                                             *"
echo "*   Downloading dependencies into /lib that are easy to get   *"
echo "*                                                             *"
echo "***************************************************************"
echo ""

download_dependency "http://repo1.maven.org/maven2/commons-httpclient/commons-httpclient/3.1/commons-httpclient-3.1.jar"
download_dependency "http://repo1.maven.org/maven2/commons-logging/commons-logging/1.1/commons-logging-1.1.jar"
download_dependency "http://repo1.maven.org/maven2/commons-codec/commons-codec/1.3/commons-codec-1.3.jar"
download_dependency "http://repo1.maven.org/maven2/joda-time/joda-time/1.5.2/joda-time-1.5.2.jar"
download_dependency "http://repo1.maven.org/maven2/commons-dbcp/commons-dbcp/1.2.2/commons-dbcp-1.2.2.jar"
download_dependency "http://repo1.maven.org/maven2/commons-pool/commons-pool/1.3/commons-pool-1.3.jar"
download_dependency "http://repository.jboss.org/maven2/org/springframework/spring/2.0.8/spring-2.0.8.jar"
download_dependency "http://mirrors.ibiblio.org/pub/mirrors/maven2/mysql/mysql-connector-java/5.0.4/mysql-connector-java-5.0.4.jar"

echo ""
echo "***************************************************************"
echo "*                                                             *"
echo "*   Download the following files yourself:                    *"
echo "*                                                             *"
echo "***************************************************************"
echo ""

echo "Download pircbot 1.4.6 from http://www.jibble.org/pircbot.php and save pircbot-1.4.6.jar into lib"
echo "Download mallet 2.0-RC3 from http://mallet.cs.umass.edu/download.php and save mallet.jar and mallet-deps.jar into lib"
