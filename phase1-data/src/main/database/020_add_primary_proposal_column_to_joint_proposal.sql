-- add a column to Proposals table so that JointProposals know which of their components is the master proposal
alter table proposals add column primary_proposal_id int;