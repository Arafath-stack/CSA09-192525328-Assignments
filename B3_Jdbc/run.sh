#!/bin/bash
# Usage: JDBC_JAR=/path/to/mysql-connector-j.jar ./run.sh [b31|b32|b33|all]
set -e
cd "$(dirname "$0")"
JAR=${JDBC_JAR:-/tmp/jdbc.jar}
mkdir -p out outputs
javac -cp "$JAR" -d out *.java
java -cp "out:$JAR" B3Experiments ${1:-all} | tee outputs/B3Experiments_${1:-all}.txt
java -cp "out:$JAR" B3Alternative | tee outputs/B3Alternative_results.csv
