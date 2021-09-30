#!/bin/bash

DATE_STR=`date --rfc-3339=date`
JARFILE="`pwd`/target/jdk-backports-monitor.jar"

java -jar "$JARFILE" --csv --parity 11 --output data/$DATE_STR-parity-11u.csv
java -jar "$JARFILE" --csv --parity 8  --output data/$DATE_STR-parity-8u.csv
java -jar "$JARFILE" --csv --parity 17 --output data/$DATE_STR-parity-17u.csv