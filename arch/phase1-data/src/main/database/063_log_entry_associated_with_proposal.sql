ALTER TABLE log_entries ADD COLUMN proposal_id INTEGER REFERENCES proposals(id) DEFAULT null;
CREATE INDEX log_entries_proposals_index ON log_entries USING btree (proposal_id);

