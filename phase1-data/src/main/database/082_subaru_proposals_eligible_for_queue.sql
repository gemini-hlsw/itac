-- Holds the Subaru proposals that are eligible for being scheduled via the Queue Engine ("schedule as Partner") --
UPDATE schema_version SET version = 82;

CREATE TABLE queue_subaru_exchange_proposals_eligible_for_queue (
  queue_id integer,
  proposal_id integer
);

ALTER TABLE queue_subaru_exchange_proposals_eligible_for_queue ADD FOREIGN KEY(queue_id) REFERENCES queues(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queue_subaru_exchange_proposals_eligible_for_queue ADD FOREIGN KEY(proposal_id) REFERENCES proposals(id) ON DELETE CASCADE DEFERRABLE;
