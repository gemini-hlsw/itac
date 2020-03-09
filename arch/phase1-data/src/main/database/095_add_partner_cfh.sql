UPDATE schema_version SET version = 95;

insert into partners values((select max(id) + 1 from partners), 'CFHT', 'CFHT', 'CFH', false, 0, 'BOTH', 'devost@cfht.hawaii.edu');
