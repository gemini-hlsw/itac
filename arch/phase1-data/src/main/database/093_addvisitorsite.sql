UPDATE schema_version SET version = 93;

alter table v2_blueprints add column visitor_site varchar(40);
update v2_blueprints set instrument='PHOENIX_GS' where instrument='PHOENIX';
update v2_blueprints set instrument='DSSI_GN' where instrument='DSSI';
update v2_blueprints set instrument='TEXES_GN' where instrument='TEXES';
