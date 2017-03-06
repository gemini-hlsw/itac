UPDATE schema_version SET version = 75;

ALTER TABLE v2_phase_i_proposals ADD COLUMN scheduling TEXT;
