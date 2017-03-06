DROP TABLE rankings CASCADE;
DROP TABLE bandings CASCADE;

CREATE TABLE bandings (
	id integer,
	queue_id integer,
	proposal_id integer,
	rank integer,
	description text
);
ALTER TABLE bandings ADD PRIMARY KEY(id);
ALTER TABLE bandings ADD FOREIGN KEY(queue_id) REFERENCES queues(id) DEFERRABLE;
ALTER TABLE bandings ADD FOREIGN KEY(proposal_id) REFERENCES proposals(id) DEFERRABLE;
