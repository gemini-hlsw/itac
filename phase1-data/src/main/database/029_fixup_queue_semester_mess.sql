ALTER TABLE semesters DROP COLUMN display_name;
ALTER TABLE semesters ADD COLUMN display_year integer;
ALTER TABLE semesters ADD COLUMN name text;

COMMENT ON COLUMN public.semesters.display_year IS 'Year of the semester (AD, ie 2011).' ;
COMMENT ON COLUMN public.semesters.name IS 'Gemini name for the semester, (A or B)' ;

ALTER TABLE bin_configurations DROP CONSTRAINT bin_configurations_semester_id_fkey;
ALTER TABLE bin_configurations DROP CONSTRAINT bin_configurations_site_id_fkey;
ALTER TABLE bin_configurations DROP COLUMN site_id;
ALTER TABLE bin_configurations DROP COLUMN semester_id;

ALTER TABLE committees ADD COLUMN semester_id integer;
COMMENT ON COLUMN committees.semester_id IS 'What semester is the committee formed to manage queues for.' ;
ALTER TABLE committees ADD CONSTRAINT ADD FOREIGN KEY(semester_id) REFERENCES semesters(id);