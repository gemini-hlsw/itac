--Drop V2 stuff if its exists
DROP TABLE IF EXISTS v2_band3s CASCADE;
DROP TABLE IF EXISTS v2_blueprints CASCADE;
DROP TABLE IF EXISTS v2_conditions CASCADE;
DROP TABLE IF EXISTS v2_coordinates CASCADE;
DROP TABLE IF EXISTS v2_ephemeris_elements CASCADE;
DROP TABLE IF EXISTS v2_gmoss_dispersers CASCADE;
DROP TABLE IF EXISTS v2_gmoss_imaging_filters CASCADE;
DROP TABLE IF EXISTS v2_guide_stars CASCADE ;
DROP TABLE IF EXISTS v2_investigators CASCADE;
DROP TABLE IF EXISTS v2_itacs CASCADE;
DROP TABLE IF EXISTS v2_keywords CASCADE;
DROP TABLE IF EXISTS v2_magnitudes CASCADE;
DROP TABLE IF EXISTS v2_observations CASCADE;
DROP TABLE IF EXISTS v2_phase_i_proposals CASCADE;
DROP TABLE IF EXISTS v2_phone_numbers CASCADE;
DROP TABLE IF EXISTS v2_proper_motions CASCADE;
DROP TABLE IF EXISTS v2_proposals_co_investigators CASCADE;
DROP TABLE IF EXISTS v2_schedulings_investigators CASCADE;
DROP TABLE IF EXISTS v2_conditions CASCADE;
DROP TABLE IF EXISTS v2_targets CASCADE;
DROP TABLE IF EXISTS schema_version CASCADE;

-- Deconnect proposals from version 1 style phase 1 proposals.
ALTER TABLE proposals DROP COLUMN p1_document_id;
ALTER TABLE proposals ADD COLUMN v2_p1_proposal_id INTEGER;
ALTER TABLE proposals ADD COLUMN joint_proposal_itac_id INTEGER;

ALTER TABLE memberships DROP COLUMN id;
ALTER TABLE memberships ADD CONSTRAINT person_committee_membership_unique UNIQUE(person_id, committee_id);

-- Counter, hopefully monotonically increasing upwards that should allow us to know the last update applied.
CREATE TABLE schema_version (
    version INTEGER
);
INSERT INTO schema_version VALUES (45);

CREATE TABLE v2_investigators (
    id INTEGER PRIMARY KEY,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    investigator_status TEXT, -- NULL for CoIs for some reason.
    institution TEXT,
    institution_address TEXT,
    country TEXT,
    type TEXT,
    created TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,
    UNIQUE(type, email)
);

CREATE TABLE v2_phase_i_proposals (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL,
    proposal_abstract TEXT NOT NULL,
    schema_version TEXT NOT NULL,
    proposal_type TEXT NOT NULL,
    too_status TEXT, -- NULL signifies not a ToO
    queue_mode BOOLEAN NOT NULL,
    classical_mode BOOLEAN NOT NULL,
    attachment TEXT,
    created TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,
    band3_id INTEGER,
    principal_investigator_id INTEGER REFERENCES v2_investigators(id),
    submissions_exchange_id INTEGER,
    submissions_itac_id INTEGER,
    submissions_key TEXT
);

CREATE TABLE v2_keywords (
    v2_phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id),
    keyword TEXT NOT NULL,
    UNIQUE(v2_phase_i_proposal_id, keyword)
);

CREATE TABLE v2_phone_numbers (
    phone_number TEXT NOT NULL,
    investigator_id INTEGER REFERENCES v2_investigators(id)
);

CREATE TABLE v2_schedulings_investigators (
    phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id) NOT NULL,
    investigators_id INTEGER REFERENCES v2_investigators(id) NOT NULL
);

CREATE TABLE v2_coordinates (
    id INTEGER PRIMARY KEY,
    type TEXT NOT NULL,
    deg_ra NUMERIC,
    deg_dec NUMERIC,
    hms_ra TEXT,
    dms_dec TEXT,
    valid_at TIMESTAMP WITH TIME ZONE,
    coordinate_id INTEGER REFERENCES v2_coordinates(id),
    epoch TEXT
);

CREATE TABLE v2_targets (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    coordinate_id INTEGER REFERENCES v2_coordinates(id),
    phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id) ,
    target_id TEXT
);

CREATE TABLE v2_magnitudes (
    id INTEGER PRIMARY KEY,
    value NUMERIC,
    band TEXT NOT NULL,
    system TEXT NOT NULL,
    target_id INTEGER REFERENCES v2_targets(id) NOT NULL
);

CREATE TABLE v2_proposals_co_investigators (
    v2_phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id) NOT NULL,
    investigator_id INTEGER REFERENCES v2_investigators(id) NOT NULL
);

CREATE TABLE v2_conditions (
    id INTEGER PRIMARY KEY,
    name TEXT,
    cloud_cover TEXT NOT NULL,
    image_quality TEXT NOT NULL,
    sky_background TEXT NOT NULL,
    water_vapor TEXT NOT NULL,
    condition_id TEXT,
    max_airmass NUMERIC,
    phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id),
    UNIQUE(name, phase_i_proposal_id)
);

CREATE TABLE v2_blueprints (
    id INTEGER PRIMARY KEY,
    type TEXT NOT NULL,
    wavelength_regime TEXT,
    name TEXT,
    blueprint_id TEXT,
    fpu TEXT,
    preimaging BOOLEAN,
    instrument TEXT,
    phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id)
);

CREATE TABLE v2_observations (
    id INTEGER PRIMARY KEY,
    time_amount_value NUMERIC NOT NULL,
    time_amount_unit TEXT NOT NULL,
    success_estimation_value NUMERIC,
    success_estimation_checksum TEXT,
    target_id INTEGER REFERENCES v2_targets(id) NOT NULL,
    condition_id INTEGER REFERENCES v2_conditions(id) NOT NULL,
    blueprint_id INTEGER REFERENCES v2_blueprints(id) NOT NULL,
    phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id) NOT NULL
);

CREATE TABLE v2_guide_stars (
    id INTEGER PRIMARY KEY,
    guider TEXT,
    observation_id INTEGER REFERENCES v2_observations(id),
    target_id INTEGER REFERENCES v2_targets(id)
);

CREATE TABLE v2_band3s (
    id INTEGER PRIMARY KEY,
    min_request_value   NUMERIC,
    min_request_units   TEXT,
    max_request_value   NUMERIC,
    max_request_units   TEXT,
    condition_id INTEGER REFERENCES v2_conditions(id)
);
ALTER TABLE v2_phase_i_proposals ADD CONSTRAINT phase_i_proposal_band_3_fk FOREIGN KEY (band3_id) REFERENCES v2_band3s(id);

CREATE TABLE v2_ngo_submissions (
    id INTEGER PRIMARY KEY,
    partner TEXT,
    request_value NUMERIC,
    request_units TEXT,
    request_min_value NUMERIC,
    request_min_units TEXT,
    request_lead_investigator_id INTEGER REFERENCES v2_investigators(id),
    receipt_timestamp TIMESTAMP WITH TIME ZONE,
    receipt_id TEXT,
    accept_email TEXT,
    accept_ranking NUMERIC,
    accept_recommend_value NUMERIC,
    accept_recommend_units TEXT,
    accept_recommend_min_value NUMERIC,
    accept_recommend_min_units TEXT,
    accept_poor_weather BOOLEAN,
    comment TEXT,
    rejected BOOLEAN DEFAULT false,
    phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id)
);

CREATE TABLE v2_exchange_submissions (
    id INTEGER PRIMARY KEY,
    partner TEXT,
    request_value NUMERIC,
    request_units TEXT,
    request_min_value NUMERIC,
    request_min_units TEXT,
    request_lead_investigator_id INTEGER REFERENCES v2_investigators(id),
    receipt_timestamp TIMESTAMP WITH TIME ZONE,
    receipt_id TEXT,
    accept_email TEXT,
    accept_ranking NUMERIC,
    accept_recommend_value NUMERIC,
    accept_recommend_units TEXT,
    accept_recommend_min_value NUMERIC,
    accept_recommend_min_units TEXT,
    accept_poor_weather BOOLEAN,
    comment TEXT,
    rejected BOOLEAN DEFAULT false
);
ALTER TABLE v2_phase_i_proposals ADD CONSTRAINT phase_i_proposal_exchange_submission_fk FOREIGN KEY (submissions_exchange_id) REFERENCES v2_exchange_submissions(id);

CREATE TABLE v2_itacs (
    id INTEGER PRIMARY KEY,
    accept_program_id TEXT,
    accept_contact TEXT,
    accept_email TEXT,
    accept_band INTEGER,
    accept_award_value NUMERIC,
    accept_award_units TEXT,
    accept_rollover BOOLEAN,
    comment TEXT,
    rejected BOOLEAN DEFAULT false
);
ALTER TABLE v2_phase_i_proposals ADD CONSTRAINT phase_i_proposal_itac_fk FOREIGN KEY (submissions_itac_id) REFERENCES v2_itacs(id);

CREATE TABLE v2_gmoss_imaging_filters (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id)
);

CREATE TABLE v2_gmoss_dispersers (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id)
);

ALTER TABLE partner_charges ADD COLUMN charge_value INTEGER;
ALTER TABLE partner_charges ADD COLUMN charge_units TEXT;
ALTER TABLE partner_charges DROP COLUMN time_id;
COMMENT ON COLUMN partner_charges.charge_value IS 'Numerical component of charge time amount';
COMMENT ON COLUMN partner_charges.charge_units IS 'Units component of charge time amount';
COMMENT ON COLUMN partner_charges.charge_type IS 'STI discriminator column, supports multiple inheritance of partner exchange, rollover, exchange, classical, available, adjustments and large programs.';