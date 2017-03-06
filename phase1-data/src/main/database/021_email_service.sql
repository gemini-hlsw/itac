-- tables that are needed for email services
DROP TABLE email_templates;

CREATE TABLE email_templates (
	id integer,
	type text,
	description text,
	template text
);
ALTER TABLE email_templates ADD PRIMARY KEY(id);

DROP TABLE emails;

CREATE TABLE emails (
	id integer,
	queue_id integer,
	banding_id integer,
	proposal_id integer,
	address text,
	subject text,
	content text,
	comment text,
	sent_timestamp timestamp,
	failed_timestamp timestamp,
	error text
);
ALTER TABLE emails ADD PRIMARY KEY(id);
