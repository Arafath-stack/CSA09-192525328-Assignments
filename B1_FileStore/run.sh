#!/bin/bash
# Compile and run every B1 program; saves console output in outputs/
set -e
cd "$(dirname "$0")"
mkdir -p out outputs
javac -d out *.java
java -cp out PassStore            | tee outputs/PassStore_output.txt
java -cp out PassStoreTest        | tee outputs/PassStoreTest_output.txt
java -cp out LineCounter          | tee outputs/LineCounter_results.csv
java -cp out GrowthBench          | tee outputs/GrowthBench_results.csv
java -cp out MemoryBench          | tee outputs/MemoryBench_results.csv
