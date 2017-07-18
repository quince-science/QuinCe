#!/bin/bash

qcRoutinesSource=$1
jarsDir=$2
output=$3


sourcePath="$(dirname ${BASH_SOURCE[0]})/../WebApp/src:$qcRoutinesSource"

javadoc -sourcepath "$sourcePath" -d "$output" -subpackages uk.ac.exeter.QuinCe:uk.ac.exeter.QCRoutines \
        -source 1.8 -private -windowtitle "QuinCe Javadoc" -doctitle "QuinCe Javadoc" \
        -classpath "${jarsDir}/joda-time.jar:${jarsDir}/servlet-api.jar:${jarsDir}/primefaces.jar:${jarsDir}/commons-email.jar" \
        -link https://docs.oracle.com/javase/8/docs/api/ \
        -link https://docs.oracle.com/javaee/7/api/ \
        -link http://www.joda.org/joda-time/apidocs/ \
        -link https://www.primefaces.org/docs/api/5.3/ \
        -link https://commons.apache.org/proper/commons-email/javadocs/api-release/
