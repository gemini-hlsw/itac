DROP TABLE exchange_partner_charges;
DROP TABLE queue_proposals_exchange;

CREATE TABLE exchange_partner_charges (
  id integer PRIMARY KEY,
  queue_id integer,
  partner_id integer,
  charge double precision
);
ALTER TABLE exchange_partner_charges ADD FOREIGN KEY(queue_id) REFERENCES queues(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE exchange_partner_charges ADD FOREIGN KEY(partner_id) REFERENCES partners(id) ON DELETE CASCADE DEFERRABLE;

CREATE TABLE queue_proposals_exchange (
  queue_id integer,
  proposal_id integer
);

ALTER TABLE queue_proposals_exchange ADD FOREIGN KEY(queue_id) REFERENCES queues(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queue_proposals_exchange ADD FOREIGN KEY(proposal_id) REFERENCES proposals(id) ON DELETE CASCADE DEFERRABLE;
