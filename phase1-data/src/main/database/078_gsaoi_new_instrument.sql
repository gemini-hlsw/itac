UPDATE schema_version SET version = 78;

CREATE TABLE v2_gsaoi_filters (
    type TEXT,
    blueprint_id INTEGER REFERENCES v2_blueprints(id) ON DELETE CASCADE
);