UPDATE schema_version SET version = 47;

ALTER TABLE v2_blueprints ADD COLUMN nod_and_shuffle BOOLEAN DEFAULT FALSE;
