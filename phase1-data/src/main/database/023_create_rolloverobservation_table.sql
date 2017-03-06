DROP TABLE IF EXISTS rollover_reports CASCADE;

CREATE TABLE rollover_reports (
    id integer not null,
    site_id integer not null,
    report_type text,
    set_name text
);

ALTER TABLE rollover_reports ADD PRIMARY KEY (id);
ALTER TABLE rollover_reports ADD FOREIGN KEY (site_id) REFERENCES sites(id);

DROP TABLE IF EXISTS rollover_observations CASCADE;

CREATE TABLE rollover_observations (
	id integer not null,
	partner_id integer not null,
	target_id integer not null,
	site_quality_id integer not null,
	time_id integer not null,
    observation_id_reference text not null,
    rollover_report_id integer not null,
    type text not null
);

ALTER TABLE rollover_observations ADD PRIMARY KEY (id);
ALTER TABLE rollover_observations ADD FOREIGN KEY (partner_id) REFERENCES partners(id) DEFERRABLE ;
ALTER TABLE rollover_observations ADD FOREIGN KEY (target_id) REFERENCES p1_targets(id) DEFERRABLE;
ALTER TABLE rollover_observations ADD FOREIGN KEY (site_quality_id) REFERENCES p1_site_qualities(id) DEFERRABLE;
ALTER TABLE rollover_observations ADD FOREIGN KEY (time_id) REFERENCES p1_times(id) DEFERRABLE;
ALTER TABLE rollover_observations ADD FOREIGN KEY (rollover_report_id) references rollover_reports(id) DEFERRABLE;

DROP TABLE IF EXISTS rollover_observations_derived CASCADE;

CREATE TABLE rollover_observations_derived (
	id integer not null,
	partner_id integer not null,
	target_id integer not null,
	site_quality_id integer not null,
	time_id integer not null,
    observation_id_reference text not null,
    rollover_report_id integer not null
);

ALTER TABLE rollover_observations_derived ADD PRIMARY KEY (id);
ALTER TABLE rollover_observations_derived ADD FOREIGN KEY (partner_id) REFERENCES partners(id) DEFERRABLE ;
ALTER TABLE rollover_observations_derived ADD FOREIGN KEY (target_id) REFERENCES p1_targets(id) DEFERRABLE;
ALTER TABLE rollover_observations_derived ADD FOREIGN KEY (site_quality_id) REFERENCES p1_site_qualities(id) DEFERRABLE;
ALTER TABLE rollover_observations_derived ADD FOREIGN KEY (time_id) REFERENCES p1_times(id) DEFERRABLE;
ALTER TABLE rollover_observations_derived ADD FOREIGN KEY (rollover_report_id) references rollover_reports(id) DEFERRABLE;