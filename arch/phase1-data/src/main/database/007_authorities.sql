DROP TABLE IF EXISTS authority_roles CASCADE;
DROP TABLE IF EXISTS authorities CASCADE;

ALTER TABLE people ADD COLUMN enabled boolean DEFAULT true;
ALTER TABLE people ADD COLUMN partner_id integer;

ALTER TABLE people ADD FOREIGN KEY(partner_id) REFERENCES partners(id);

CREATE TABLE authority_roles (
    id integer PRIMARY KEY,
    rolename text
);

-- Join table authority_roles M:M people
CREATE TABLE authorities (
    person_id integer NOT NULL,
    role_id integer NOT NULL
);

ALTER TABLE authorities ADD PRIMARY KEY(person_id, role_id);

