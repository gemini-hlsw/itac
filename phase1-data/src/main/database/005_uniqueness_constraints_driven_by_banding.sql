ALTER TABLE queues ADD CONSTRAINT queues_equality_key UNIQUE(committee_id, name);
ALTER TABLE bandings ADD CONSTRAINT bandings_equality_key UNIQUE(queue_id, proposal_id);
ALTER TABLE proposals ADD CONSTRAINT proposals_equality_key UNIQUE(committee_id, partner_id, p1_document_id);
ALTER TABLE partners ADD CONSTRAINT partners_country_key UNIQUE(partner_country_key);
ALTER TABLE committees ADD CONSTRAINT committees_name_key UNIQUE(name);
