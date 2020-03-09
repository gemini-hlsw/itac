-- this table stores all issues that have been found for a proposal during the last checker run
DROP TABLE proposal_issues;

CREATE TABLE proposal_issues (
	id integer,
	proposal_id integer,
	severity integer,
	message text
);
ALTER TABLE proposal_issues ADD PRIMARY KEY(id);
CREATE INDEX proposal_issues_proposals_index ON proposal_issues(proposal_id);