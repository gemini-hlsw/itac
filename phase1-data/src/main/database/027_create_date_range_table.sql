DROP TABLE IF EXISTS date_ranges CASCADE;

CREATE TABLE date_ranges (
    id integer not null,
    "start_date" date not null,
    "end_date" date not null,
    parent_id integer
);

ALTER TABLE date_ranges ADD PRIMARY KEY (id);

DROP TABLE IF EXISTS blackouts cascade;

CREATE TABLE blackouts (
    id integer not null,
    date_range_id integer not null,
    resource_id integer not null
);

ALTER TABLE blackouts add PRIMARY KEY (id);