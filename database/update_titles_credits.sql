-- Course titles and credits (CE Level 200).
-- Run after seed_timetable.sql. Titles for other levels can be added the same way.

USE classcue;

UPDATE courses SET title = 'General Psychology', credits = 1 WHERE code = 'CE 251';
UPDATE courses SET title = 'Differential Equations', credits = 3 WHERE code = 'CE 265';
UPDATE courses SET title = 'Computer Architecture and Organisation', credits = 3 WHERE code = 'CE 271';
UPDATE courses SET title = 'Computer Graphics', credits = 2 WHERE code = 'CE 273';
UPDATE courses SET title = 'Discrete Mathematics', credits = 2 WHERE code = 'CE 275';
UPDATE courses SET title = 'Programming with Java', credits = 2 WHERE code = 'CE 277';
UPDATE courses SET title = 'Advanced Database Systems', credits = 2 WHERE code = 'CE 279';
UPDATE courses SET title = 'Digital Systems and Design', credits = 3 WHERE code = 'CE 281';

SELECT code, title, credits FROM courses WHERE level = 200 AND programme = 'CE' ORDER BY code;
