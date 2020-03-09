-- Support schema changes adding exchange telescope blueprints.

ALTER TABLE v2_phase_i_proposals ADD COLUMN exchange_instrument VARCHAR;