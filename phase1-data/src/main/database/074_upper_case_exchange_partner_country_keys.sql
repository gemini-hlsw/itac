-- Originally this was done as migration 062, but there was a numbering conflict

UPDATE schema_version SET version = 74;
-- upper case for country keys is preferable to make it easier to compare them with enum names
UPDATE partners SET partner_country_key='SUBARU' where id=78;
UPDATE partners SET partner_country_key='KECK' where id=79;