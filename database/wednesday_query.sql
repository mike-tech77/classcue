USE classcue;

select c.code, s.group_label, s.start_time, s.end_time, s.venue, s.lecturer
from timetable_slots s
join courses c ON c.id = s.course_id
where s.day = 'Wednesday'
order by s.start_time;
