UPDATE schema_version SET version = 90;

alter table v2_blueprints add column fibers varchar(40);