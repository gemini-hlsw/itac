UPDATE schema_version SET version = 88;

alter table queues drop column lptac_time;
drop table lptac_participants;
