-- add a column to flag partner countries (in contrast to proposal sources like Keck, Subaru and Gemini staff)
alter table partners add column is_country boolean not null default true;

-- update in case we are running this on a database with an already populated partners table
update partners set is_country = false where abbreviation in ('UH', 'Subaru', 'Keck', 'GeminiStaff');
