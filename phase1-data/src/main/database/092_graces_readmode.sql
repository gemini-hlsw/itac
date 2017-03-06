UPDATE schema_version SET version = 92;

alter table v2_blueprints add column readMode varchar(40);