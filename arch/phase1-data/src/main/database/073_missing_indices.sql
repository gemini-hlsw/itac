UPDATE schema_version SET version = 73;

create index queue_committee_id_index on queues(committee_id);
create index partner_sequence_committee_id_index on partner_sequence(committee_id);
create index bandings_queue_id_index on bandings(queue_id);
