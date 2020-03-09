UPDATE schema_version SET version = 86;

alter table v2_blueprints add column central_wavelength_range varchar(40);