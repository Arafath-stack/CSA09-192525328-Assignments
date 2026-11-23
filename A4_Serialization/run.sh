#!/bin/bash
# A4 demos: transient, object-graph, serialVersionUID versioning, and format comparison
set -e
cd "$(dirname "$0")"
rm -rf build; mkdir -p build outputs
{
echo "===== 1. Why Connection must be transient ====="
mkdir -p build/common; javac -d build/common src/common/DemoTransient.java
java -cp build/common DemoTransient

for mode in explicit implicit; do
  echo
  echo "===== 2. Versioning test: serialVersionUID = $mode ====="
  for v in v1 v2; do
    mkdir -p build/$mode/$v
    cp src/common/Room.java src/common/Hostel.java build/$mode/$v/
    cp src/$v/*.java build/$mode/$v/
    if [ $mode = explicit ]; then sed -i 's|//UID//|private static final long serialVersionUID = 1L;|' build/$mode/$v/Resident.java
    else sed -i 's|//UID//||' build/$mode/$v/Resident.java; fi
    javac -d build/$mode/$v build/$mode/$v/*.java
  done
  java -cp build/$mode/v1 WriteOld build/$mode/hostel.ser
  java -cp build/$mode/v2 ReadNew build/$mode/hostel.ser
done
echo
echo "===== 3. Java serialization vs CSV vs JSON ====="
javac -d build/common src/common/FormatBench.java
java -cp build/common FormatBench
} 2>&1 | grep -v "Picked up" | tee outputs/A4_output.txt
