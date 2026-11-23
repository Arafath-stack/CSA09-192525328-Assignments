#!/bin/bash
# Usage: JDBC_JAR=/path/to/mysql-connector-j.jar ./run.sh
set -e
cd "$(dirname "$0")"
JAR=${JDBC_JAR:-/tmp/jdbc.jar}
CP="out:$JAR:../B3_Jdbc/out:../B1_FileStore/out"
mkdir -p out outputs
javac -cp "$JAR:../B3_Jdbc/out:../B1_FileStore/out" -d out *.java
java -cp "$CP" GenerateLegacyFile
java -cp "$CP" Migrator        | tee outputs/Migrator_output.txt
java -cp "$CP" MetaReport      | tee outputs/MetaReport_output.txt
java -cp "$CP" ConcurrencyDemo | tee outputs/ConcurrencyDemo_output.txt
java -cp "$CP" -Xmx2g ScaleTest | tee outputs/ScaleTest_output.txt
