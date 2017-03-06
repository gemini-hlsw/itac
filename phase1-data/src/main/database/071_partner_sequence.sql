UPDATE schema_version SET version = 71;

DROP TABLE IF EXISTS partner_sequence CASCADE;

create TABLE partner_sequence (
  id integer not null,
  committee_id integer not null,
  name text not null,
  csv text not null,
  repeat boolean DEFAULT true
);

ALTER table partner_sequence ADD PRIMARY KEY (id);

ALTER TABLE committees ADD COLUMN partner_sequence_id INT;