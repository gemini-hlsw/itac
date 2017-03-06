ALTER TABLE v2_coordinates DROP COLUMN epoch;

DROP TABLE IF EXISTS v2_proper_motions;

CREATE TABLE v2_proper_motions (
    id INTEGER PRIMARY KEY,
    delta_ra NUMERIC,
    delta_dec NUMERIC
);

ALTER TABLE v2_targets ADD COLUMN epoch TEXT NOT NULL;
ALTER TABLE v2_targets ADD COLUMN type TEXT NOT NULL;
ALTER TABLE v2_targets ADD COLUMN proper_motion_id INTEGER REFERENCES v2_proper_motions(id);
ALTER TABLE v2_targets ADD COLUMN proper_motion_id INTEGER REFERENCES v2_proper_motions(id);
ALTER TABLE v2_targets ALTER coordinate_id DROP NOT NULL;

CREATE TABLE v2_ephemeris_elements (
    id INTEGER PRIMARY KEY,
    magnitude NUMERIC,
    valid_at TIMESTAMP WITH TIME ZONE,
    coordinate_id INTEGER REFERENCES v2_coordinates(id),
    target_id INTEGER REFERENCES v2_targets(id) NOT NULL
);
