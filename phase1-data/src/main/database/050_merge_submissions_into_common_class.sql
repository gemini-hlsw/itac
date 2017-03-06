UPDATE schema_version SET version = 50;

drop table v2_ngo_submissions cascade;
drop table v2_exchange_submissions cascade;

--Drop the key into the old table
ALTER TABLE v2_phase_i_proposals drop CONSTRAINT phase_i_proposal_exchange_submission_fk;

DROP TABLE IF EXISTS v2_submissions;

CREATE TABLE v2_submissions (
    id INTEGER PRIMARY KEY,
    partner TEXT,
    request_value NUMERIC,
    request_units TEXT,
    request_min_value NUMERIC,
    request_min_units TEXT,
    request_lead_investigator_id INTEGER REFERENCES v2_investigators(id),
    receipt_timestamp TIMESTAMP WITH TIME ZONE,
    receipt_id TEXT,
    accept_email TEXT,
    accept_ranking NUMERIC,
    accept_recommend_value NUMERIC,
    accept_recommend_units TEXT,
    accept_recommend_min_value NUMERIC,
    accept_recommend_min_units TEXT,
    accept_poor_weather BOOLEAN,
    comment TEXT,
    rejected BOOLEAN DEFAULT false,
    type TEXT,
    phase_i_proposal_id INTEGER REFERENCES v2_phase_i_proposals(id)
);

--Add the constraint to the new table
ALTER TABLE v2_phase_i_proposals rename column submissions_exchange_id to submission_id;

ALTER TABLE v2_phase_i_proposals add CONSTRAINT phase_i_proposal_submission_fk FOREIGN KEY (submission_id) REFERENCES v2_submissions(id);
