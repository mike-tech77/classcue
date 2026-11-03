-- ClassCue seed data: CE Level 200, Semester 1, 2026/2027
-- Source: CE200 timetable PDF (released 1 Nov 2026), checked against the school Excel file.
-- group_label is NULL when the whole class attends together. Times are 24-hour.

USE classcue;

-- Courses (safe to run even if already inserted: code is UNIQUE)
INSERT IGNORE INTO courses (code, programme, level) VALUES
('CE 251', 'CE', 200),
('CE 265', 'CE', 200),
('CE 271', 'CE', 200),
('CE 273', 'CE', 200),
('CE 275', 'CE', 200),
('CE 277', 'CE', 200),
('CE 279', 'CE', 200),
('CE 281', 'CE', 200);

-- Timetable slots
INSERT INTO timetable_slots (course_id, group_label, day, start_time, end_time, venue, lecturer) VALUES
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 271'), NULL, 'Monday', '06:30:00', '08:30:00', 'SR 13', 'DR T. KWANTWI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 251'), NULL, 'Monday', '13:00:00', '14:00:00', 'Online', 'REV I. KWOFIE'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 277'), NULL, 'Monday', '17:00:00', '18:00:00', 'Online', 'E. OBU'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 271'), 'A', 'Tuesday', '06:30:00', '08:30:00', 'Computer Lab', 'DR T. KWANTWI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 271'), 'B', 'Tuesday', '08:30:00', '10:30:00', 'Computer Lab', 'DR T. KWANTWI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 279'), NULL, 'Tuesday', '10:30:00', '12:30:00', 'Online', 'DR R. N. NKETSIAH'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 273'), NULL, 'Tuesday', '13:00:00', '15:00:00', 'Online', 'DR E. M. MARTEY'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 277'), 'A', 'Tuesday', '18:00:00', '19:00:00', 'SR 7B', 'E. OBU'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 279'), NULL, 'Wednesday', '06:30:00', '08:30:00', 'Main Auditorium', 'DR R. N. NKETSIAH'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 277'), 'B', 'Wednesday', '10:30:00', '12:30:00', 'SR 5A', 'E. OBU'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 273'), 'A', 'Wednesday', '10:30:00', '12:30:00', 'Computer Lab', 'DR E. M. MARTEY'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 281'), NULL, 'Wednesday', '13:00:00', '15:00:00', 'SR 13', 'DR T. KWANTWI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 265'), NULL, 'Wednesday', '15:00:00', '17:00:00', 'Main Auditorium', 'DR D. A. ARHINFUL'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 275'), NULL, 'Thursday', '06:30:00', '07:30:00', 'Main Auditorium', 'DR ALEX NTI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 251'), NULL, 'Thursday', '07:30:00', '08:30:00', 'Online', 'REV I. KWOFIE'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 277'), 'A', 'Thursday', '08:30:00', '10:30:00', 'Computer Lab', 'E. OBU'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 281'), 'A', 'Thursday', '10:30:00', '12:30:00', 'Computer Lab', 'DR T. KWANTWI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 281'), 'B', 'Thursday', '13:00:00', '15:00:00', 'Computer Lab', 'DR T. KWANTWI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 273'), 'B', 'Thursday', '17:00:00', '19:00:00', 'Computer Lab', 'DR E. M. MARTEY'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 277'), 'B', 'Friday', '06:30:00', '08:30:00', 'Computer Lab', 'E. OBU'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 279'), NULL, 'Friday', '10:30:00', '12:30:00', 'Hardware Lab', 'DR R. N. NKETSIAH'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 275'), NULL, 'Friday', '15:00:00', '17:00:00', 'Main Auditorium', 'DR ALEX NTI'),
                                                                                                     ((SELECT id FROM courses WHERE code = 'CE 265'), NULL, 'Friday', '17:00:00', '19:00:00', 'SR 13', 'DR D. A. ARHINFUL');

-- Check: expect 23 rows
SELECT COUNT(*) AS total_slots FROM timetable_slots;