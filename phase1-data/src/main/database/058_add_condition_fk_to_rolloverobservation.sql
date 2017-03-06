UPDATE schema_version SET version = 58;

ALTER TABLE rollover_observations ADD COLUMN condition_id INT;
ALTER TABLE rollover_observations add CONSTRAINT rollover_observations_conditions_fk FOREIGN KEY (condition_id) REFERENCES v2_conditions(id);

ALTER TABLE rollover_observations ADD COLUMN time_amount_value NUMERIC NOT NULL;
ALTER TABLE rollover_observations ADD COLUMN time_amount_unit TEXT NOT NULL;

ALTER TABLE rollover_observations DROP COLUMN site_quality_id;
ALTER table rollover_observations DROP COLUMN time_id;

