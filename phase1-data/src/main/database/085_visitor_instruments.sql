UPDATE schema_version SET version = 85;

alter table v2_blueprints add column custom_name varchar;