#!/bin/bash

## Don't forget to do 'mvn package' after source code changes

DATE_STR=`date --rfc-3339=date`
JARFILE="`pwd`/target/jdk-backports-monitor.jar"

java -jar "$JARFILE" --verbose --parity 11 --output-prefix data/$DATE_STR-parity-11u
java -jar "$JARFILE" --verbose --parity 8  --output-prefix data/$DATE_STR-parity-8u
java -jar "$JARFILE" --verbose --parity 17 --output-prefix data/$DATE_STR-parity-17u
java -jar "$JARFILE" --verbose --parity 21 --output-prefix data/$DATE_STR-parity-21u