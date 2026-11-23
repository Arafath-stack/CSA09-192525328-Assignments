#!/bin/bash
# Usage: JDBC_JAR=/path/to/mysql-connector-j.jar ./run.sh   (database must be running; see ../sql/create_test_db.sql)
set -e
cd "$(dirname "$0")"
JAR=${JDBC_JAR:-/tmp/jdbc.jar}
mkdir -p out outputs
javac -cp "$JAR:../B1_FileStore/out" -d out *.java
java -cp "out:$JAR:../B1_FileStore/out" LoginDemo       | tee outputs/LoginDemo_output.txt
java -cp "out:$JAR:../B1_FileStore/out" DamagedFileDemo | tee outputs/DamagedFileDemo_output.txt
