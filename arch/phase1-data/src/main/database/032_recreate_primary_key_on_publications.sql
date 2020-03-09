ALTER TABLE p1_publications DROP CONSTRAINT p1_publications_pkey;
CREATE UNIQUE index p1_publications_pkey ON p1_publications (md5(proposal_support_id||text||order_nr));

