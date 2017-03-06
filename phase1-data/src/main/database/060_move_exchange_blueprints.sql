-- Support schema changes adding exchange telescope blueprints.
-- in step 055 this field was added to the wrong table, remove it from v2_phase_i_proposals and add it to v2_blueprints

ALTER TABLE v2_phase_i_proposals DROP COLUMN exchange_instrument;
ALTER TABLE v2_blueprints ADD COLUMN exchange_instrument VARCHAR;