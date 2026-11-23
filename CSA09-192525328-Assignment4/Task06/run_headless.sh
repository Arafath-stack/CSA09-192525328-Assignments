#!/bin/sh
# Compiles Task 6 and runs the headless demo (18 checks) and the load benchmark. No display and no MySQL needed (in-memory DAO).
set -e
rm -rf out && javac -d out $(find src -name "*.java")
java -cp out Demo
java -Xmx3g -cp out LoadBench
