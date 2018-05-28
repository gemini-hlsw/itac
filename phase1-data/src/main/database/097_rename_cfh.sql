UPDATE schema_version SET version = 97;

update partners set name = 'CFH' where name = 'CFHT';
update partners set partner_country_key = 'CFH' where name = 'CFH';
