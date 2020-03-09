ALTER TABLE v2_observations DROP COLUMN success_estimation_value;
ALTER TABLE v2_observations DROP COLUMN success_estimation_checksum;

ALTER TABLE v2_observations ADD COLUMN meta_checksum VARCHAR;
ALTER TABLE v2_observations ADD COLUMN meta_gsa NUMERIC;
ALTER TABLE v2_observations ADD COLUMN meta_target_visibility VARCHAR;
ALTER TABLE v2_observations ADD COLUMN meta_guiding_estimation_percentage INTEGER;
ALTER TABLE v2_observations ADD COLUMN meta_guiding_target_visibility VARCHAR;
