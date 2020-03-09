-- Need to be able to set the committee to null prior to delete of shutdown --
UPDATE schema_version SET version = 81;

ALTER TABLE shutdowns ALTER COLUMN committee_id DROP not null;