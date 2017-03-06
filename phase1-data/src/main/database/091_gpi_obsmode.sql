UPDATE schema_version SET version = 91;

alter table v2_blueprints add column obsMode varchar(40);