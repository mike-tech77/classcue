-- ClassCue database schema
-- Safe to run again: it only creates what is missing.
CREATE DATABASE IF NOT EXISTS classcue;
USE classcue;

-- One row per course, e.g. CE 271 (programme CE, level 200)
CREATE TABLE IF NOT EXISTS courses (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(10) NOT NULL UNIQUE,
  title VARCHAR(100),
  programme VARCHAR(5) NOT NULL,
  level INT NOT NULL,
  credits INT
);

-- One row per class meeting. group_label is NULL when the whole class attends together.
CREATE TABLE IF NOT EXISTS timetable_slots (
  id INT AUTO_INCREMENT PRIMARY KEY,
  course_id INT NOT NULL,
  group_label VARCHAR(5),
  day VARCHAR(10) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  venue VARCHAR(50),
  lecturer VARCHAR(60),
  FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- One row per student profile (no password yet)
CREATE TABLE IF NOT EXISTS students (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(30) NOT NULL UNIQUE,
  programme VARCHAR(5) NOT NULL,
  level INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- The courses (and groups) a student chose. group_label is empty for courses without groups.
CREATE TABLE IF NOT EXISTS registrations (
  student_id INT NOT NULL,
  course_code VARCHAR(10) NOT NULL,
  group_label VARCHAR(5) NOT NULL DEFAULT '',
  PRIMARY KEY (student_id, course_code),
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

-- A student's study-plan settings
CREATE TABLE IF NOT EXISTS preferences (
  student_id INT PRIMARY KEY,
  weekly_hours INT NOT NULL,
  time_window VARCHAR(10) NOT NULL,
  free_days VARCHAR(80) NOT NULL DEFAULT '',
  focus_courses VARCHAR(200) NOT NULL DEFAULT '',
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);
