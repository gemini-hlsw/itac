-- Expanded RA restrictions from REL-1043
UPDATE schema_version SET version = 84;

insert into ra_bin_sizes values(212, 3, 0, 0, 0);
insert into ra_bin_sizes values(213, 4, 0, 0, 0);

insert into bin_configurations values((select max(id) + 1 from bin_configurations), '3h/20d', 212, 221, 5, 'f');
insert into bin_configurations values((select max(id) + 1 from bin_configurations), '3h/30d', 212, 220, 5, 'f');
insert into bin_configurations values((select max(id) + 1 from bin_configurations), '4h/20d', 213, 221, 5, 'f');
insert into bin_configurations values((select max(id) + 1 from bin_configurations), '4h/30d', 213, 220, 5, 'f');