#!/bin/sh
# Run ONE block at a time, after you have compiled, run and checked that task yourself.
# (The faculty check the commit history, so do not run everything in one go on the last day.)
# First time only:
#   git init && git branch -M main
#   git remote add origin https://github.com/Arafath-stack/CSA09-192525328-Assignment4.git
git add README.md SCREENSHOT_CHECKLIST.md .gitignore commit_progressively.sh tools && git commit -m "Add README, helper tools and repository layout"
git add Task01 && git commit -m "Task 1: diagnose and repair the fee window (FeeService, SwingWorker)"
git add Task02 && git commit -m "Task 2: outpatient appointment desk, event table and trace"
git add Task03 && git commit -m "Task 3: library search screen layouts and resize tests"
git add Task04 && git commit -m "Task 4: DrawPad defects, repair and @Override check"
git add Task05 && git commit -m "Task 5: four event-handling styles and the Converter"
git add Task06 && git commit -m "Task 6: event registration application (Swing, JDBC, SQL script)"
git add charts && git commit -m "Add graphs used in the report"
git push -u origin main
