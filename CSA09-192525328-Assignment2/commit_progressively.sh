#!/bin/sh
# Run ONE block at a time, after you have compiled, run and checked that question yourself.
# (The faculty check the commit history, so do not run everything in one go on the last day.)
# First time only:
#   git init && git branch -M main
#   git remote add origin https://github.com/Arafath-stack/CSA09-192525328-Assignment2.git
git add README.md .gitignore commit_progressively.sh && git commit -m "Add README and repository layout"
git add Q1 && git commit -m "Q1: String vs StringBuilder vs StringBuffer experiments"
git add Q2 && git commit -m "Q2: ArrayList vs LinkedList vs ArrayDeque experiments"
git add Q3 && git commit -m "Q3: hashCode/equals contract tests and completed Attendance"
git add Q4 && git commit -m "Q4: HashMap/TreeMap/LinkedHashMap pharmacy inventory"
git add Q5 && git commit -m "Q5: generic leaderboard and comparator tests"
git add Q6 && git commit -m "Q6: triage priority queue, starvation and fairness"
git add Q7 charts && git commit -m "Q7: diagnose and redesign the pharmacy store, validation data and charts"
git push -u origin main
