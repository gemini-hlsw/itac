UPDATE schema_version SET version = 70;

CREATE TABLE broadcast_messages (
	id integer,
	message text,
	created timestamp with time zone DEFAULT now()
);
ALTER TABLE broadcast_messages ADD PRIMARY KEY(id);
CREATE INDEX broadcast_messages_created on broadcast_messages USING btree(created);
