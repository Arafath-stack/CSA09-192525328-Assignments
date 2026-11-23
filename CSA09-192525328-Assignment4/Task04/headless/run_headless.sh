#!/bin/sh
# Headless verification of Task 4 (no display needed).
# Derives JPanel "twins" of the real AWT classes (same listener and paint code, see ../../tools/make_twin.py), then runs the demo.
set -e
rm -rf work && mkdir work
python3 ../../tools/make_twin.py ../buggy/DrawPad.java DrawPadBuggyT work
python3 ../../tools/make_twin.py ../DrawPad.java DrawPadFixedT work
cp DrawPadDemo.java ../../tools/Shot.java ../../tools/Twin.java work/
cd work && javac -d out *.java && java -cp out DrawPadDemo
