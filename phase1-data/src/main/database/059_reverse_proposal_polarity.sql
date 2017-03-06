ALTER TABLE v2_phase_i_proposals ADD COLUMN proposal_id INTEGER REFERENCES proposals(id);
ALTER TABLE proposals DROP COLUMN v2_p1_proposal_id;
