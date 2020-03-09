DROP TABLE exchange_partner_charges;
DROP TABLE partner_charges;
DROP TABLE queue_proposals_classical;

-- make partner charges more generic - will cover large program, classical and rollover in addition to exchange charges
CREATE TABLE partner_charges (
  id integer PRIMARY KEY,
  queue_id integer,
  partner_id integer,
  time_id integer,
  charge_type text
);
ALTER TABLE partner_charges ADD FOREIGN KEY(queue_id) REFERENCES queues(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE partner_charges ADD FOREIGN KEY(partner_id) REFERENCES partners(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE partner_charges ADD FOREIGN KEY(time_id) REFERENCES p1_times(id) ON DELETE CASCADE DEFERRABLE;

-- add relationship between a queue and all classical proposals that influenced it's calculation
CREATE TABLE queue_proposals_classical (
  queue_id integer,
  proposal_id integer
);
ALTER TABLE queue_proposals_exchange ADD FOREIGN KEY(queue_id) REFERENCES queues(id) ON DELETE CASCADE DEFERRABLE;
ALTER TABLE queue_proposals_exchange ADD FOREIGN KEY(proposal_id) REFERENCES proposals(id) ON DELETE CASCADE DEFERRABLE;
