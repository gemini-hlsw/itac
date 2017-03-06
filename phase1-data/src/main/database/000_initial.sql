DROP TABLE committees CASCADE;
DROP TABLE people CASCADE;
DROP TABLE memberships CASCADE;
DROP TABLE log_entries CASCADE;
DROP TABLE log_entry_types CASCADE;
DROP TABLE log_notes CASCADE;
DROP TABLE queues CASCADE;
DROP TABLE lptac_participants CASCADE;
DROP TABLE partners CASCADE;
DROP TABLE queue_notes CASCADE;
DROP TABLE queue_band_restriction_rules CASCADE;
DROP TABLE restricted_bins CASCADE;
DROP TABLE band_restriction_rules CASCADE;
DROP TABLE sites CASCADE;
DROP TABLE semesters CASCADE;
DROP TABLE ra_bin_sizes CASCADE;
DROP TABLE dec_bin_sizes CASCADE;
DROP TABLE condition_sets CASCADE;
DROP TABLE conditions CASCADE;
DROP TABLE bin_configurations CASCADE;
DROP TABLE proposals CASCADE;
DROP TABLE queue_restricted_bins CASCADE;
DROP TABLE rankings CASCADE;

DROP SEQUENCE hibernate_sequence;

-- Initially, we're going with a universal id
CREATE SEQUENCE hibernate_sequence START WITH 1000000;


CREATE TABLE conditions (
	id integer,
	condition_set_id integer,
	name text,
	image_quality text,
	sky_background text,
	cloud_cover text,
	water_vapor text,
	available_percentage integer
);
ALTER TABLE conditions ADD PRIMARY KEY(id);

CREATE TABLE committees (
	id integer,
	name text,
	active boolean DEFAULT false
);
ALTER TABLE committees ADD PRIMARY KEY(id);

CREATE TABLE people (
	id integer,
	name text,
	password text
);
ALTER TABLE people ADD PRIMARY KEY(id);

CREATE TABLE partners (
	id integer,
	name text,
	partner_country_key text,
	abbreviation text
);
ALTER TABLE partners ADD PRIMARY KEY(id);

CREATE TABLE sites (
	id integer,
	display_name text
);
ALTER TABLE sites ADD PRIMARY KEY(id);

CREATE TABLE semesters (
	id integer,
	display_name text
);
ALTER TABLE semesters ADD PRIMARY KEY(id);

CREATE TABLE memberships (
	id integer,
	person_id integer,
	committee_id integer
);
ALTER TABLE memberships ADD PRIMARY KEY(id);
ALTER TABLE memberships ADD FOREIGN KEY(committee_id) REFERENCES committees(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE memberships ADD FOREIGN KEY(person_id) REFERENCES people(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE ra_bin_sizes (
	id integer,
	hours integer,
	minutes integer,
	seconds integer,
	fractions_of_seconds  integer
);
ALTER TABLE ra_bin_sizes ADD PRIMARY KEY(id);

CREATE TABLE dec_bin_sizes (
	id integer,
	degrees integer,
	minutes integer,
	seconds integer,
	fractions_of_seconds integer
);
ALTER TABLE dec_bin_sizes ADD PRIMARY KEY(id); 

CREATE TABLE bin_configurations (
	id integer,
	name text,
	site_id integer,
	semester_id integer,
	ra_bin_size_id integer,
	dec_bin_size_id integer,
	smoothing_length integer,
	editable boolean
);
ALTER TABLE bin_configurations ADD PRIMARY KEY(id);
ALTER TABLE bin_configurations ADD FOREIGN KEY(site_id) REFERENCES sites(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE bin_configurations ADD FOREIGN KEY(semester_id) REFERENCES semesters(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE bin_configurations ADD FOREIGN KEY(ra_bin_size_id) REFERENCES ra_bin_sizes(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE bin_configurations ADD FOREIGN KEY(dec_bin_size_id) REFERENCES dec_bin_sizes(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE condition_sets (
	id integer,
	name text
);
ALTER TABLE condition_sets ADD PRIMARY KEY(id);

CREATE TABLE queues (
	id integer,
	committee_id integer,
	site_id integer,
	total_time_available integer,
	partner_with_initial_pick_id integer,
	partner_quanta integer,
	condition_set_id integer,
	bin_configuration_id integer,
	lptac_time integer,
	band_1_cutoff integer,
	band_2_cutoff integer,
	band_3_cutoff integer,
	band_3_conditions_threshold integer,
	use_after_band_3 boolean,
	name text
);
ALTER TABLE queues ADD PRIMARY KEY(id);
ALTER TABLE queues ADD FOREIGN KEY(committee_id) REFERENCES committees(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queues ADD FOREIGN KEY(site_id) REFERENCES sites(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queues ADD FOREIGN KEY(partner_with_initial_pick_id) REFERENCES partners(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queues ADD FOREIGN KEY(condition_set_id) REFERENCES condition_sets(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queues ADD FOREIGN KEY(bin_configuration_id) REFERENCES bin_configurations(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE band_restriction_rules (
	id integer,
	rule_type text,
	name text
);
ALTER TABLE band_restriction_rules ADD PRIMARY KEY(id);

CREATE TABLE restricted_bins (
	id integer,
	restricted_bin_type text,
	description text,
	units text,
	value integer
);
ALTER TABLE restricted_bins ADD PRIMARY KEY(id);

CREATE TABLE queue_restricted_bins (
	queue_id integer,
	restricted_bin_id integer
);
ALTER TABLE queue_restricted_bins ADD PRIMARY KEY(queue_id, restricted_bin_id);
ALTER TABLE queue_restricted_bins ADD FOREIGN KEY(queue_id) REFERENCES queues(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queue_restricted_bins ADD FOREIGN KEY(restricted_bin_id) REFERENCES restricted_bins(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE queue_band_restriction_rules (
	queue_id integer,
	band_restriction_rule_id integer
);
ALTER TABLE queue_band_restriction_rules ADD PRIMARY KEY(queue_id, band_restriction_rule_id);
ALTER TABLE queue_band_restriction_rules ADD FOREIGN KEY(queue_id) REFERENCES queues(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queue_band_restriction_rules ADD FOREIGN KEY(band_restriction_rule_id) REFERENCES band_restriction_rules(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE queue_notes (
	id integer,
	queue_id integer,
	note text
);
ALTER TABLE queue_notes ADD PRIMARY KEY(id);
ALTER TABLE queue_notes ADD FOREIGN KEY(queue_id) REFERENCES queues(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE lptac_participants (
	queue_id integer NOT NULL,
	partner_id integer NOT NULL
);
ALTER TABLE lptac_participants ADD PRIMARY KEY(queue_id, partner_id);
ALTER TABLE lptac_participants ADD FOREIGN KEY(queue_id) REFERENCES queues(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE lptac_participants ADD FOREIGN KEY(partner_id) REFERENCES partners(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE log_entries (
	id integer,
	created_at timestamp WITH time zone,
	updated_at timestamp WITH time zone,
	message text,
	queue_id integer,
	committee_id integer
);
ALTER TABLE log_entries ADD PRIMARY KEY(id);
ALTER TABLE log_entries ADD FOREIGN KEY(queue_id) REFERENCES queues(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE log_entries ADD FOREIGN KEY(committee_id) REFERENCES committees(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE log_entry_types (
	type text,
	log_entry_id integer
);
ALTER TABLE log_entry_types ADD PRIMARY KEY(type, log_entry_id);
ALTER TABLE log_entry_types ADD FOREIGN KEY(log_entry_id) REFERENCES log_entries(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE log_notes (
	id integer,
	note_text text,
	log_entry_id integer
);
ALTER TABLE log_notes ADD PRIMARY KEY(id);
ALTER TABLE log_notes ADD FOREIGN KEY(log_entry_id) REFERENCES log_entries(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE proposals (
	id integer,
	committee_id integer,
	partner_id integer,
	p1_document_id integer
);
ALTER TABLE proposals ADD PRIMARY KEY(id);
ALTER TABLE proposals ADD FOREIGN KEY(partner_id) REFERENCES partners(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE proposals ADD FOREIGN KEY(committee_id) REFERENCES committees(id)  ON DELETE CASCADE DEFERRABLE;

CREATE TABLE rankings (
	id integer,
	queue_id integer,
	proposal_id integer,
	ranking integer
);
ALTER TABLE rankings ADD PRIMARY KEY(id);
ALTER TABLE rankings ADD FOREIGN KEY(queue_id) REFERENCES queues(id)  ON DELETE CASCADE DEFERRABLE;
ALTER TABLE rankings ADD FOREIGN KEY(proposal_id) REFERENCES proposals(id)  ON DELETE CASCADE DEFERRABLE;
