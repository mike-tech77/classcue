# ClassCue

A web app that helps students draft their specific class timetable from the school's general timetable and also help them build a personal timetable with it based on how they want it to be.

## Problem
The school publishes one general timetable for everyone, so each student has to pick out their own courses by hand and work out where their free time is. This is slow and error-prone, and it leads to missed classes and poorly planned study time.

## Solution
ClassCue reads the school's general timetable and extracts only the classes a student is registered for, giving them a clean class schedule. It then asks the student about their preferences, such as study hours per week, preferred times of day, hardest courses, and days to keep free. From those answers it builds a personal timetable that fits study blocks around their classes.

## Tech
Java (server) · MySQL (database) · C++ (scheduling engine) · HTML/CSS/JS (interface)

## Status
Phase 1: database design (in progress)
Planned: class filtering, clash detection, personal timetable generator