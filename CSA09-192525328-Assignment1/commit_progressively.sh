#!/bin/sh
# Run ONE block at a time, after you have compiled, run and checked that task yourself.
# (The faculty check the commit history, so do not run everything in one go on the last day.)
# First time only:
#   git init && git branch -M main
#   git remote add origin https://github.com/Arafath-stack/CSA09-192525328-Assignment1.git
git add README.md .gitignore commit_progressively.sh && git commit -m "Add README and repository layout"
git add Task01 && git commit -m "Task 1: problem analysis notes"
git add Task02 && git commit -m "Task 2: data types and casting demo"
git add Task03 && git commit -m "Task 3: complete ParkingFacility (allocate/release)"
git add Task04 && git commit -m "Task 4: constructors, chaining, copy constructor"
git add Task05 && git commit -m "Task 5: static counter and object life cycle"
git add Task06 && git commit -m "Task 6: inheritance and constructor order trace"
git add Task07 && git commit -m "Task 7: overriding, equals and hashCode"
git add Task08 && git commit -m "Task 8: complete Vehicle hierarchy and FeeDemo"
git add Task09 && git commit -m "Task 9: abstraction (abstract class and interfaces)"
git add Task10 && git commit -m "Task 10: encapsulation and wrapper classes"
git add Task11 && git commit -m "Task 11: complete system, tests and benchmarks"
git add Task12 && git commit -m "Task 12: extended design (e-bike, van, one-day pass)"
git push -u origin main
