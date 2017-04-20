UPDATE schema_version SET version = 94;

alter table v2_observations add column prog_time_amount_value numeric not null default 0;
alter table v2_observations add column prog_time_amount_unit text not null default 'HR';
alter table v2_observations add column part_time_amount_value numeric not null default 0;
alter table v2_observations add column part_time_amount_unit text not null default 'HR';
