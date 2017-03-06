UPDATE schema_version SET version = 53;

DROP TABLE IF EXISTS v2_niri_filters CASCADE;
DROP TABLE IF EXISTS v2_flamingos2_filters CASCADE;
DROP TABLE IF EXISTS v2_nici_red_filters CASCADE;
DROP TABLE IF EXISTS v2_nici_blue_filters CASCADE;
DROP TABLE IF EXISTS v2_trecs_filters CASCADE;

ALTER TABLE v2_phase_i_proposals DROP COLUMN proposal_type; -- Used to be an enum.
ALTER TABLE v2_phase_i_proposals ADD COLUMN type VARCHAR NOT NULL; -- Polymorphic proposals look to be the most robust way to model the changes from the last sprint.
ALTER TABLE v2_phase_i_proposals RENAME COLUMN proposal_abstract TO abstract; -- Cleaning up schema

ALTER TABLE v2_phase_i_proposals ADD COLUMN tac_category VARCHAR NOT NULL;
COMMENT ON COLUMN v2_phase_i_proposals.tac_category IS 'A proposal has a TAC category of Solar System, Galactic, or Extragalactic.';

ALTER TABLE v2_phase_i_proposals RENAME COLUMN submissions_itac_id TO itac_id; -- Submissions element is disappearing, so keeping the schema consistent and non-surprising.

ALTER TABLE v2_phase_i_proposals RENAME COLUMN submission_id TO exchange_submission_id; -- Probably always should have had this name.

-- Band 3 is back!
ALTER TABLE v2_phase_i_proposals ADD COLUMN band3_time_value NUMERIC;
ALTER TABLE v2_phase_i_proposals ADD COLUMN band3_time_units VARCHAR;
ALTER TABLE v2_phase_i_proposals ADD COLUMN band3_min_time_value NUMERIC;
ALTER TABLE v2_phase_i_proposals ADD COLUMN band3_min_time_units VARCHAR;
ALTER TABLE v2_phase_i_proposals ADD COLUMN request_lead_investigator_id INTEGER;
ALTER TABLE v2_phase_i_proposals ADD COLUMN exchange_partner VARCHAR;
ALTER TABLE v2_phase_i_proposals ADD COLUMN too_option VARCHAR;
ALTER TABLE v2_phase_i_proposals ADD COLUMN special_submission_id INTEGER;
ALTER TABLE v2_phase_i_proposals ADD COLUMN comment VARCHAR;

ALTER TABLE v2_phase_i_proposals DROP COLUMN too_status;
ALTER TABLE v2_phase_i_proposals DROP COLUMN queue_mode;
ALTER TABLE v2_phase_i_proposals DROP COLUMN classical_mode;



ALTER TABLE v2_schedulings_investigators RENAME TO v2_phase_i_proposals_classical_investigators;

ALTER TABLE v2_submissions ADD COLUMN special_proposal_type VARCHAR;
ALTER TABLE v2_submissions DROP COLUMN partner;
ALTER TABLE v2_submissions ADD COLUMN partner_id INTEGER;

ALTER TABLE v2_gmoss_imaging_filters RENAME TO v2_gmoss_filters;
ALTER TABLE v2_gmosn_imaging_filters RENAME TO v2_gmosn_filters;

DROP TABLE IF EXISTS v2_gmoss_dispersers;
DROP TABLE IF EXISTS v2_gmosn_dispersers;

CREATE TABLE v2_niri_filters (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id)
);

CREATE TABLE v2_flamingos2_filters (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id)
);

CREATE TABLE v2_nici_red_filters (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id)
);

CREATE TABLE v2_nici_blue_filters (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id)
);

CREATE TABLE v2_trecs_filters (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id)
);

ALTER TABLE v2_blueprints ADD COLUMN camera VARCHAR;
ALTER TABLE v2_blueprints ADD COLUMN dichroic VARCHAR;
ALTER TABLE v2_blueprints ADD COLUMN fpm VARCHAR;