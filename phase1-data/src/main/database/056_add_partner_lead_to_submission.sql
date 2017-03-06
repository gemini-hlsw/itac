-- Incorporate changes related to exchange and ngo submissions.

ALTER TABLE v2_submissions ADD COLUMN partner_lead_id INTEGER;
ALTER TABLE v2_submissions ADD FOREIGN KEY(partner_lead_id) REFERENCES v2_investigators(id);






















