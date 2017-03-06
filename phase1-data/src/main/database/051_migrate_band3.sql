UPDATE schema_version SET version = 52;

-- Bring in late changes that move band 3 from a proposal attribute into a per-observation flag.
-- Bring in additional blueprints and attending paraphernalia
DROP TABLE IF EXISTS v2_band3s CASCADE;

ALTER TABLE v2_band3s ADD COLUMN band TEXT NOT NULL;
ALTER TABLE v2_phase_i_proposals DROP COLUMN band3_id;

ALTER TABLE v2_blueprints ADD COLUMN pixel_scale TEXT;
ALTER TABLE v2_blueprints ADD COLUMN altair_configuration TEXT;
ALTER TABLE v2_blueprints ADD COLUMN disperser TEXT;
ALTER TABLE v2_blueprints ADD COLUMN cross_disperser TEXT;
ALTER TABLE v2_blueprints ADD COLUMN filter TEXT;
ALTER TABLE v2_blueprints ADD COLUMN polarimetry TEXT;
ALTER TABLE v2_blueprints ADD COLUMN occulting_disk TEXT;

ALTER TABLE v2_observations ADD COLUMN band TEXT;

CREATE TABLE v2_michelle_imaging_filters (
    type TEXT,
    blueprint_id INTEGER
);

CREATE TABLE v2_gmosn_imaging_filters (
    type TEXT,
    blueprint_id INTEGER
);
