-- Drop everything and start afresh
DROP TABLE p1_documents CASCADE;
DROP TABLE p1_proposal_keys CASCADE;
DROP TABLE p1_commons CASCADE;
DROP TABLE p1_investigators_container CASCADE;
DROP TABLE p1_investigators CASCADE;
DROP TABLE p1_sites CASCADE;
DROP TABLE p1_addresses CASCADE;
DROP TABLE p1_contacts CASCADE;
DROP TABLE p1_emails CASCADE;
DROP TABLE p1_phones CASCADE;
DROP TABLE p1_faxes CASCADE;
DROP TABLE p1_keyword_sets CASCADE;
DROP TABLE p1_keywords CASCADE;
DROP TABLE p1_attachment_sets CASCADE;
DROP TABLE p1_attachments CASCADE;
DROP TABLE p1_target_catalogs CASCADE;
DROP TABLE p1_targets CASCADE;
DROP TABLE p1_coordinate_systems CASCADE;
DROP TABLE p1_referent_supports CASCADE;
DROP TABLE p1_observatories CASCADE;
DROP TABLE p1_observation_lists CASCADE;
DROP TABLE p1_times CASCADE;
DROP TABLE p1_proposal_supports CASCADE;
DROP TABLE p1_publications CASCADE;
DROP TABLE p1_allocations CASCADE;
DROP TABLE p1_schedulings CASCADE;
DROP TABLE p1_optimal_date_ranges CASCADE;
DROP TABLE p1_itac_scheduled_date_ranges CASCADE;
DROP TABLE p1_impossible_date_ranges CASCADE;
DROP TABLE p1_sync_observing_date_ranges CASCADE;
DROP TABLE p1_resource_lists CASCADE;
DROP TABLE p1_resource_categories CASCADE;
DROP TABLE p1_resources CASCADE;
DROP TABLE p1_resource_components CASCADE;
DROP TABLE p1_resource_component_names CASCADE;
DROP TABLE p1_observatory_extensions CASCADE;
DROP TABLE p1_extensions CASCADE;
DROP TABLE p1_gemini_allocation_extensions CASCADE;
DROP TABLE p1_gemini_band3_extensions CASCADE;
DROP TABLE p1_itac_extensions CASCADE;
DROP TABLE p1_sub_details_extensions CASCADE;
DROP TABLE p1_tac_extensions CASCADE;
-- DROP TABLE p1_gemini_parts CASCADE;
DROP TABLE p1_submissions CASCADE;
DROP TABLE p1_host_countries CASCADE;
DROP TABLE p1_partner_submissions CASCADE;
DROP TABLE p1_partner_countries CASCADE;
DROP TABLE p1_submission_country_partner_submissions CASCADE;
DROP TABLE p1_observations CASCADE;
DROP TABLE p1_reference_list_supports CASCADE;
DROP TABLE p1_references CASCADE;
DROP TABLE p1_site_qualities CASCADE;
DROP TABLE p1_requirements CASCADE;

DROP SEQUENCE hibernate_sequence;

-- Initially, we're going with a universal id
CREATE SEQUENCE hibernate_sequence START WITH 10000;

-----------------------------------------------------
-- P1Document persistence
CREATE TABLE p1_documents (
	id integer,
	parent_id integer,
	parent_class text,
	created timestamp with time zone,
	last_modified timestamp with time zone,
	proposal_key_id integer,
	common_id integer
);
ALTER TABLE p1_documents ADD PRIMARY KEY (id);

-----------------------------------------------------
-- P1ProposalKey persistence
CREATE TABLE p1_proposal_keys (
	id integer,
	value text NOT NULL
);
ALTER TABLE p1_proposal_keys ADD PRIMARY KEY (id);

-- P1ProposalKey constraints
ALTER TABLE p1_documents ADD FOREIGN KEY (proposal_key_id) REFERENCES p1_proposal_keys(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Common persistence
CREATE TABLE p1_commons (
	id integer,
	parent_id integer,
	parent_class text,
	abstract text,
	title text,
	version text,
	investigators_container_id integer,
	keyword_set_id integer,
	scientific_justification_id integer,
	target_catalog_id integer
);
ALTER TABLE p1_commons ADD PRIMARY KEY (id);
-- Common constraints
ALTER TABLE p1_documents ADD FOREIGN KEY (common_id) REFERENCES p1_commons(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Investigators container persistence
CREATE TABLE p1_investigators_container (
	id integer,
	parent_id integer,
	parent_class text,
	primary_investigator_id integer
);
ALTER TABLE p1_investigators_container ADD PRIMARY KEY (id);
-- Investigators container constraints
ALTER TABLE p1_commons ADD FOREIGN KEY (investigators_container_id) REFERENCES p1_investigators_container(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Investigators persistence
CREATE TABLE p1_investigators (
	id integer,
	parent_id integer,
	parent_class text,
	type text,
	visiting boolean,
	first_name text,
	last_name text,
	institution text,
	status text,
	site_id integer,
	investigators_container_id integer,
	contact_id integer
);
ALTER TABLE p1_investigators ADD PRIMARY KEY (id);
-- Investigators constraints
ALTER TABLE p1_investigators ADD FOREIGN KEY (investigators_container_id) REFERENCES p1_investigators_container(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_investigators_container ADD FOREIGN KEY (primary_investigator_id) REFERENCES p1_investigators(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Sites persistence
CREATE TABLE p1_sites (
	id integer,
	parent_id integer,
	parent_class text,
	country text,
	institution text,
	addresses text,
	contact_id integer
);
ALTER TABLE p1_sites ADD PRIMARY KEY (id);
-- Sites constraints
ALTER TABLE p1_investigators ADD FOREIGN KEY (site_id) REFERENCES p1_sites(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Addresses persistence
CREATE TABLE p1_addresses (
	address text,
	parent_id integer,
	parent_class text,
	site_id integer
);
ALTER TABLE p1_addresses ADD PRIMARY KEY(address, site_id);
-- Addresses constraints
ALTER TABLE p1_addresses ADD FOREIGN KEY (site_id) REFERENCES p1_sites(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Contacts persistence
CREATE TABLE p1_contacts (
	id integer,
	parent_id integer,
	parent_class text
);
ALTER TABLE p1_contacts ADD PRIMARY KEY (id);
-- Contacts constraints
ALTER TABLE p1_sites ADD FOREIGN KEY (contact_id) REFERENCES p1_contacts(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_investigators ADD FOREIGN KEY (contact_id) REFERENCES p1_contacts(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Emails persistence
CREATE TABLE p1_emails (
	email text,
	contact_id integer
);
ALTER TABLE p1_emails ADD PRIMARY KEY(email, contact_id);
--  Emails constraint
ALTER TABLE p1_emails ADD FOREIGN KEY (contact_id) REFERENCES p1_contacts(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- Faxes persistence
CREATE TABLE p1_faxes (
	fax text,
	contact_id integer
);
ALTER TABLE p1_faxes ADD PRIMARY KEY(fax, contact_id);
-- Faxes constraints
ALTER TABLE p1_faxes ADD FOREIGN KEY (contact_id) REFERENCES p1_contacts(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- phones persistence
CREATE TABLE p1_phones (
	phone text,
	contact_id integer
);
ALTER TABLE p1_phones ADD PRIMARY KEY(phone, contact_id);
-- Phones constraints
ALTER TABLE p1_phones ADD FOREIGN KEY (contact_id) REFERENCES p1_contacts(id) ON DELETE CASCADE DEFERRABLE;


-----------------------------------------------------
-- keyword_sets persistence
CREATE TABLE p1_keyword_sets (
	id integer,
	parent_id integer,
	parent_class text,
	category text,
	common_id integer
);
ALTER TABLE p1_keyword_sets ADD PRIMARY KEY (id);
-----------------------------------------------------
-- keyword_sets constraints
ALTER TABLE p1_commons ADD FOREIGN KEY (keyword_set_id) REFERENCES p1_keyword_sets(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- keywords persistence
CREATE TABLE p1_keywords (
	keyword text,
	keyword_set_id integer
);
ALTER TABLE p1_keywords ADD PRIMARY KEY(keyword, keyword_set_id);
-- keywords constraints
ALTER TABLE p1_keywords ADD FOREIGN KEY (keyword_set_id) REFERENCES p1_keyword_sets(id) ON DELETE CASCADE DEFERRABLE;


-----------------------------------------------------
-- attachment_sets  persistence
CREATE TABLE p1_attachment_sets (
	id integer,
	parent_id integer,
	parent_class text,
	text text
);
ALTER TABLE p1_attachment_sets ADD PRIMARY KEY (id);
-- attachment_set constraints
ALTER TABLE p1_commons ADD FOREIGN KEY (scientific_justification_id) REFERENCES p1_attachment_sets(id) ON DELETE CASCADE DEFERRABLE;

-----------------------------------------------------
-- attachments persistence
CREATE TABLE p1_attachments (
	id integer,
	parent_id integer,
	parent_class text,
	name text,
	source text,
	file_type text,
	attachment_set_id integer
);
ALTER TABLE p1_attachments ADD PRIMARY KEY (id);
-- attachments constraints
ALTER TABLE p1_attachments ADD FOREIGN KEY (attachment_set_id) REFERENCES p1_attachment_sets(id) ON DELETE CASCADE DEFERRABLE;


-----------------------------------------------------
-- target catalogs  persistence
CREATE TABLE p1_target_catalogs (
	id integer,
	parent_id integer,
	parent_class text
);
ALTER TABLE p1_target_catalogs ADD PRIMARY KEY (id);
-- target catalogs constraints
ALTER TABLE p1_commons ADD FOREIGN KEY (target_catalog_id) REFERENCES p1_target_catalogs(id) ON DELETE CASCADE DEFERRABLE;

-- target persistence
CREATE TABLE p1_targets (
	id integer,
	parent_id integer,
	parent_class text,
	name text,
	brightness text,
	target_type text,
	target_catalog_id integer,
	coordinate_system_id integer,
	referent_support_id integer 
);
ALTER TABLE p1_targets ADD PRIMARY KEY (id);
-- target catalogs constraints
ALTER TABLE p1_targets ADD FOREIGN KEY (target_catalog_id) REFERENCES p1_target_catalogs(id) ON DELETE CASCADE DEFERRABLE;


-- coordinate systems persistence
CREATE TABLE p1_coordinate_systems (
	id integer,
	parent_id integer,
	parent_class text,
	type text,
	ra text,
	dec text,
	valid_date timestamp with time zone,
	taiz timestamp with time zone,
	system_option text,
	c1 text,
	c2 text,
	c1_tracking_rate text,
	c2_tracking_Rate text,
	epoch_key text,
	epoch_value text,
	pm1_key text,
	pm1_value text,
	pm2_key text,
	pm2_value text,
	rv_key text,
	rv_value text,
	parallax_key text,
	parallax_value text,
	anode_key text,
	anode_value text,
	aq_key text,
	aq_value text,
	e text,
	inclination_key text,
	inclination_value text,
	lm_key text,
	lm_value text,
	n_key text,
	n_value text,
	perihelion_key text,
	perihelion_value text
);
ALTER TABLE p1_coordinate_systems ADD PRIMARY KEY (id);
-- coordinate system constraints
ALTER TABLE p1_targets ADD FOREIGN KEY (coordinate_system_id) REFERENCES p1_coordinate_systems(id) ON DELETE CASCADE DEFERRABLE;

-- referent support persistence
CREATE TABLE p1_referent_supports (
	id integer,
	referent_support_referent_id text,
	referent_class text,
	referent_id integer
);
ALTER TABLE p1_referent_supports ADD PRIMARY KEY (id);
-- referent supports constraints
ALTER TABLE p1_targets ADD FOREIGN KEY (referent_support_id) REFERENCES p1_referent_supports(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_observatories (
	id integer,
	parent_id integer,
	parent_class text,
	observatory_id text,
	document_id integer,
	site_id integer,
	observation_list_id integer,
	proposal_support_id integer,
	scheduling_id integer,
	technical_justification_id integer,
	observatory_extension_id integer,
	resource_list_id integer,
	requirement_id integer,
	observing_mode text,
	too_trigger text
);
ALTER TABLE p1_observatories ADD PRIMARY KEY (id);
ALTER TABLE p1_observatories ADD FOREIGN KEY (document_id) REFERENCES p1_documents(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observatories ADD FOREIGN KEY (technical_justification_id) REFERENCES p1_attachment_sets(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observatories ADD FOREIGN KEY (site_id) REFERENCES p1_sites(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_times (
	id integer,
	time_amount double precision,
	unit text
);
ALTER TABLE p1_times ADD PRIMARY KEY (id);

CREATE TABLE p1_observation_lists (
	id integer,
	parent_id integer,
	parent_class text,
	requested_time_id integer,
	constraint_list_id integer,
	resource_list_id integer

);
ALTER TABLE p1_observation_lists ADD PRIMARY KEY (id);
ALTER TABLE p1_observation_lists ADD FOREIGN KEY (requested_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observatories ADD FOREIGN KEY (observation_list_id) REFERENCES p1_observation_lists(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_proposal_supports (
	id integer,
	parent_id integer,
	parent_class text
);
ALTER TABLE p1_proposal_supports ADD PRIMARY KEY (id);
ALTER TABLE p1_observatories ADD FOREIGN KEY (proposal_support_id) REFERENCES p1_proposal_supports(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_publications (
	proposal_support_id integer NOT NULL,
	text text NOT NULL
);
ALTER TABLE p1_publications ADD PRIMARY KEY (proposal_support_id, text);
ALTER TABLE p1_publications ADD FOREIGN KEY (proposal_support_id) REFERENCES p1_proposal_supports(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_allocations (
	id integer,
	parent_id integer,
	parent_class text,
	proposal_support_id integer,
	reference text,
	awarded_time_id integer,
	percent_useful text,
	comment text
);
ALTER TABLE p1_allocations ADD PRIMARY KEY (id);
ALTER TABLE p1_allocations ADD FOREIGN KEY (proposal_support_id) REFERENCES p1_proposal_supports(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_allocations ADD FOREIGN KEY (awarded_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_schedulings (
	id integer,
	parent_id integer,
	parent_class text,
	impossible_comment text,
	optimal_comment text,
	sync_comment text,
	min_allocation_time_id integer,
	future_time_id integer
);
ALTER TABLE p1_schedulings ADD PRIMARY KEY (ID);
ALTER TABLE p1_schedulings ADD FOREIGN KEY (min_allocation_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_schedulings ADD FOREIGN KEY (future_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observatories ADD FOREIGN KEY (scheduling_id) REFERENCES p1_schedulings(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_optimal_date_ranges (
	start_date timestamp with time zone,
	end_date timestamp with time zone,
	scheduling_id integer
);
ALTER TABLE p1_optimal_date_ranges ADD PRIMARY KEY(start_date, end_date, scheduling_id);
ALTER TABLE p1_optimal_date_ranges ADD FOREIGN KEY (scheduling_id) REFERENCES p1_schedulings(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_itac_scheduled_date_ranges (
	start_date timestamp with time zone,
	end_date timestamp with time zone,
	itac_extension_id integer
);
ALTER TABLE p1_itac_scheduled_date_ranges ADD PRIMARY KEY(start_date, end_date, itac_extension_id);

CREATE TABLE p1_impossible_date_ranges (
	start_date timestamp with time zone,
	end_date timestamp with time zone,
	scheduling_id integer
);
ALTER TABLE p1_impossible_date_ranges ADD PRIMARY KEY(start_date, end_date, scheduling_id);
ALTER TABLE p1_impossible_date_ranges ADD FOREIGN KEY (scheduling_id) REFERENCES p1_schedulings(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_sync_observing_date_ranges (
	start_date timestamp with time zone,
	end_date timestamp with time zone,
	scheduling_id integer
);
ALTER TABLE p1_sync_observing_date_ranges ADD FOREIGN KEY (scheduling_id) REFERENCES p1_schedulings(id) ON DELETE CASCADE DEFERRABLE;


CREATE TABLE p1_resource_categories (
	id integer,
	parent_id integer,
	parent_class text,
	resource_list_id integer,
	resource_type text
);
ALTER TABLE p1_resource_categories ADD PRIMARY KEY(id);

CREATE TABLE p1_resources (
	id integer,
	parent_id integer,
	parent_class text,
	resource_category_id integer,
	referent_support_id integer,
	name text,
	category_type text,
	nickname text,
	string_id text

);
ALTER TABLE p1_resources ADD PRIMARY KEY(id);
ALTER TABLE p1_resources ADD FOREIGN KEY(resource_category_id) REFERENCES p1_resource_categories(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_resources ADD FOREIGN KEY(referent_support_id) REFERENCES p1_referent_supports(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_resource_components (
	id integer,
	parent_id integer,
	parent_class text,
	resource_id integer,
	resource_component_id integer,
	resource_component_type text
);
ALTER TABLE p1_resource_components ADD PRIMARY KEY (id);
ALTER TABLE p1_resource_components ADD FOREIGN KEY (resource_id) REFERENCES p1_resources(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_resource_components ADD FOREIGN KEY (resource_component_id) REFERENCES p1_resource_components(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_resource_component_names (
	resource_component_id integer,
	name text
);
ALTER TABLE p1_resource_component_names ADD PRIMARY KEY (resource_component_id, name);
ALTER TABLE p1_resource_component_names ADD FOREIGN KEY (resource_component_id) REFERENCES p1_resource_components(id) ON DELETE CASCADE DEFERRABLE;


CREATE TABLE p1_observatory_extensions (
	id integer,
	type text,
	parent_id integer,
	parent_class text,
	sub_details_extension_id integer,
	itac_extension_id integer,
	gemini_band3_extension_id integer,
	gemini_allocation_extension_id integer	
);
ALTER TABLE p1_observatory_extensions ADD PRIMARY KEY (id);
ALTER TABLE p1_observatories ADD FOREIGN KEY (observatory_extension_id) REFERENCES p1_observatory_extensions(id) ON DELETE CASCADE DEFERRABLE;

-- CREATE TABLE p1_gemini_parts (
	-- observatory_extension_id integer,
	-- sub_details_extension_id integer,
	-- itac_extension_id integer,
	-- gemini_band3_extension_id integer,
	-- gemini_allocation_extension_id integer	
-- );
-- ALTER TABLE p1_gemini_parts ADD FOREIGN KEY (observatory_extension_id) REFERENCES p1_observatory_extensions(id);

CREATE TABLE p1_extensions (
	id integer,
	parent_id integer,
	parent_class text
);
ALTER TABLE p1_extensions ADD PRIMARY KEY (id);

CREATE TABLE p1_gemini_allocation_extensions (
	extension_id integer,
	related_proposals_info text
);
ALTER TABLE p1_gemini_allocation_extensions ADD PRIMARY KEY(extension_id);
ALTER TABLE p1_gemini_allocation_extensions ADD FOREIGN KEY (extension_id) REFERENCES p1_extensions(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_gemini_band3_extensions (
	extension_id integer,
	minimum_usable_time_id integer,
	requested_time_id integer,
	consideration_text text,
	override_suggested_value boolean,
	is_usable_in_band3 boolean,
	constraint_reference_id integer
);
ALTER TABLE p1_gemini_band3_extensions ADD PRIMARY KEY(extension_id);
ALTER TABLE p1_gemini_band3_extensions ADD FOREIGN KEY (extension_id) REFERENCES p1_extensions(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_itac_extensions (
	extension_id integer UNIQUE,
	gemini_reference text,
	contact_scientist text,
	queue_band integer,
	awarded_time_id integer,
	itac_comment text,
	gemini_comment text,
	gemini_contact_scientist_email text,
	rollover boolean
);
ALTER TABLE p1_itac_extensions ADD FOREIGN KEY (extension_id) REFERENCES p1_extensions(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_itac_extensions ADD FOREIGN KEY (awarded_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_itac_scheduled_date_ranges ADD FOREIGN KEY (itac_extension_id) REFERENCES p1_itac_extensions(extension_id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_sub_details_extensions (
	extension_id integer PRIMARY KEY,
	submission_id integer,
	gemini_received_date timestamp with time zone
);
ALTER TABLE p1_sub_details_extensions ADD FOREIGN KEY (extension_id) REFERENCES p1_extensions(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_tac_extensions (
	extension_id integer PRIMARY KEY,
	observatory_extension_id integer,
	partner_ranking text,
	partner_time_id integer,
	partner_minimum_time_id integer,
	partner_comment text,
	partner_received_date timestamp with time zone,
	partner_support_email text,
	poor_weather_flag boolean,
	partner_type_code integer,
	partner_name text,
	partner_key text,
	partner_iso_country_code text,
	partner_is_exclusive boolean,
	partner_is_valid boolean
);
ALTER TABLE p1_tac_extensions ADD FOREIGN KEY (extension_id) REFERENCES p1_extensions(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_tac_extensions ADD FOREIGN KEY (observatory_extension_id) REFERENCES p1_observatory_extensions(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_tac_extensions ADD FOREIGN KEY (partner_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_tac_extensions ADD FOREIGN KEY (partner_minimum_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_resource_lists (
	extension_id integer PRIMARY KEY
);
ALTER TABLE p1_observatories ADD FOREIGN KEY(resource_list_id) REFERENCES p1_extensions(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_resource_categories ADD FOREIGN KEY (resource_list_id) REFERENCES p1_extensions(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_submissions (
	id integer,
	parent_id integer,
	parent_class text,
	total_requested_time double precision,
	total_minimum_requested_time double precision,
	unit text,
	host_type_code integer,
	host_name text,
	host_key text,
	host_iso_country_code text,
	host_is_exclusive boolean,
	host_is_valid boolean
);
ALTER TABLE p1_submissions ADD PRIMARY KEY (id);
ALTER TABLE p1_sub_details_extensions ADD FOREIGN KEY (submission_id) REFERENCES p1_submissions(id) ON DELETE CASCADE DEFERRABLE;

--  currently unused?
CREATE TABLE p1_host_countries (
	submission_id integer,
	type_code integer,
	name text,
	key text,
	iso_country_code text,
	exclusive boolean,
	valid boolean
);
ALTER TABLE p1_host_countries ADD FOREIGN KEY (submission_id) REFERENCES p1_submissions(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_partner_submissions (
	id integer,
	parent_id integer,
	parent_class text,
	requested_time double precision,
	minimum_requested_time double precision,
	reference_number text,
	flag boolean,
	submission_status text,
	contact_first_name text,
	contact_last_name text,
	partner_type_code integer,
	partner_name text,
	partner_key text,
	partner_iso_country_code text,
	partner_is_exclusive boolean,
	partner_is_valid boolean
);
ALTER TABLE p1_partner_submissions ADD PRIMARY KEY (id);

CREATE TABLE p1_submission_country_partner_submissions (
	submission_id integer,
	partner_submission_id integer,
	type_code integer,
	name text,
	key text,
	iso_country_code text,
	exclusive boolean,
	valid boolean,
	partner_country_id integer
);
ALTER TABLE p1_submission_country_partner_submissions ADD PRIMARY KEY(submission_id, partner_submission_id);

CREATE TABLE p1_partner_countries (
	type_code integer,
	name text,
	key text,
	iso_country_code text,
	exclusive boolean,
	valid boolean
);
ALTER TABLE p1_partner_countries ADD PRIMARY KEY(key);

CREATE TABLE p1_observations (
	id integer,
	parent_id integer,
	parent_class text,
	total_time_id integer,
	exposure_time_id integer,
	observation_list_id integer,
	observation_constraint_list_support_id integer,
	observation_resource_list_support_id integer,
	observation_target_list_support_id integer
);
ALTER TABLE p1_observations ADD PRIMARY KEY (id);
ALTER TABLE p1_observations ADD FOREIGN KEY (exposure_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observations ADD FOREIGN KEY (total_time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observations ADD FOREIGN KEY (observation_list_id) REFERENCES p1_observation_lists(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_reference_list_supports (
	id integer,
	property_name text
);
ALTER TABLE p1_reference_list_supports ADD PRIMARY KEY (id);
ALTER TABLE p1_observations ADD FOREIGN KEY (observation_constraint_list_support_id) REFERENCES p1_reference_list_supports(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observations ADD FOREIGN KEY (observation_resource_list_support_id) REFERENCES p1_reference_list_supports(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_observations ADD FOREIGN KEY (observation_target_list_support_id) REFERENCES p1_reference_list_supports(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_references (
	id integer,
	parent_id integer,
	parent_class text,
	state integer,
	reference_list_support_id integer,
	referent_id integer,
	referent_support_id integer,
	referent_class text
);
ALTER TABLE p1_references ADD PRIMARY KEY (id);
ALTER TABLE p1_references ADD FOREIGN KEY (reference_list_support_id) REFERENCES p1_reference_list_supports(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_site_qualities (
	id integer,
	parent_id integer,
	parent_class text,
	name text,
	cloud_cover_key text,
	image_quality_key text,
	sky_background_key text,
	water_vapor_key text,
	observatory_extension_id integer,
	referent_support_id integer
);
ALTER TABLE p1_site_qualities ADD PRIMARY KEY (id);
ALTER TABLE p1_site_qualities ADD FOREIGN KEY (observatory_extension_id) REFERENCES p1_observatory_extensions(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE p1_site_qualities ADD FOREIGN KEY (referent_support_id) REFERENCES p1_referent_supports(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE p1_requirements (
	id integer,
	parent_id integer,
	parent_class text,
	travel_needs text,
	staff_support text
);
ALTER TABLE p1_requirements ADD PRIMARY KEY(id);
