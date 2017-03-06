UPDATE schema_version SET version = 49;

ALTER TABLE bandings ADD COLUMN awarded_time_value NUMERIC;
ALTER TABLE bandings ADD COLUMN awarded_time_units TEXT;
ALTER TABLE bandings DROP COLUMN awarded_time_id; 