-- ClassCue database schema
CREATE DATABASE IF NOT EXISTS classcue;
USE classcue;

-- One row per course, e.g. CE 271 (programme CE, level 200)
CREATE TABLE courses (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         code VARCHAR(10) NOT NULL UNIQUE,
                         title VARCHAR(100),
                         programme VARCHAR(5) NOT NULL,
                         level INT NOT NULL
);

-- One row per class meeting. group_label is NULL when the whole class attends together.
CREATE TABLE timetable_slots (
                                 id INT AUTO_INCREMENT PRIMARY KEY,
                                 course_id INT NOT NULL,
                                 group_label VARCHAR(5),
                                 day VARCHAR(10) NOT NULL,
                                 start_time TIME NOT NULL,
                                 end_time TIME NOT NULL,
                                 venue VARCHAR(50),
                                 lecturer VARCHAR(100),
                                 FOREIGN KEY (course_id) REFERENCES courses(id)
);