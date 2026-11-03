-- ClassCue database schema (starter: edit the columns to fit your department)
CREATE DATABASE IF NOT EXISTS classcue;
USE classcue;

CREATE TABLE courses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL
);

CREATE TABLE timetable_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    day VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    venue VARCHAR(50),
    lecturer VARCHAR(100),
    FOREIGN KEY (course_id) REFERENCES courses(id)
);
