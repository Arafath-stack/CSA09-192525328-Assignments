#!/bin/sh
# Headless verification of Task 5 (no display needed): derives JPanel twins of every JFrame class and drives them with button clicks / Enter.
set -e
rm -rf work && mkdir work
for n in ConvA ConvB ConvC ConvD ConvA2 ConvB2 ConvC2 ConvD2 Converter; do python3 ../../tools/make_twin.py ../$n.java ${n}T work; done
cp Task5Tests.java ../ConverterActionPanel.java ../../tools/Shot.java ../../tools/Twin.java work/
cd work && javac -d out *.java && java -cp out Task5Tests
