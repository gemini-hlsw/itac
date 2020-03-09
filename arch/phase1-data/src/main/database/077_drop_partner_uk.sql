UPDATE schema_version SET version = 77;

-- UK is no longer a partner.  This also adds cascade triggers to lots of relationships that should have had them all along.

DELETE FROM people
    WHERE people.partner_id = (SELECT partners.id FROM partners WHERE partners.partner_country_key = 'UK');
DELETE FROM rollover_observations ro
    WHERE ro.partner_id = (SELECT partners.id FROM partners WHERE partners.partner_country_key = 'UK');
DELETE FROM log_entries le
    WHERE le.proposal_id IN (SELECT p.id FROM proposals p INNER JOIN partners prt ON (p.partner_id = prt.id) WHERE prt.partner_country_key = 'UK');

ALTER TABLE v2_phase_i_proposals DROP CONSTRAINT v2_phase_i_proposals_proposal_id_fkey;
ALTER TABLE v2_phase_i_proposals DROP CONSTRAINT phase_i_proposal_itac_fk;
ALTER TABLE v2_phase_i_proposals DROP CONSTRAINT exchange_submission_id;

ALTER TABLE v2_blueprints DROP CONSTRAINT v2_blueprints_phase_i_proposal_id_fkey;
ALTER TABLE v2_conditions DROP CONSTRAINT v2_conditions_phase_i_proposal_id_fkey;
ALTER TABLE v2_keywords DROP CONSTRAINT v2_keywords_v2_phase_i_proposal_id_fkey;
ALTER TABLE v2_observations DROP CONSTRAINT v2_observations_phase_i_proposal_id_fkey;
ALTER TABLE v2_phase_i_proposals_classical_investigators DROP CONSTRAINT v2_schedulings_investigators_phase_i_proposal_id_fkey;
ALTER TABLE v2_submissions DROP CONSTRAINT v2_submissions_phase_i_proposal_id_fkey;
ALTER TABLE v2_targets DROP CONSTRAINT v2_targets_phase_i_proposal_id_fkey;
ALTER TABLE bandings DROP CONSTRAINT bandings_proposal_id_fkey;
ALTER TABLE v2_proposals_co_investigators DROP CONSTRAINT v2_proposals_co_investigators_v2_phase_i_proposal_id_fkey;
ALTER TABLE v2_gmoss_filters DROP CONSTRAINT v2_gmoss_imaging_filters_blueprint_id_fkey;
ALTER TABLE v2_nici_blue_filters DROP CONSTRAINT v2_nici_blue_filters_blueprint_id_fkey;
ALTER TABLE v2_nici_red_filters DROP CONSTRAINT v2_nici_red_filters_blueprint_id_fkey;
ALTER TABLE v2_niri_filters DROP CONSTRAINT v2_niri_filters_blueprint_id_fkey;
ALTER TABLE v2_magnitudes DROP CONSTRAINT v2_magnitudes_target_id_fkey;
ALTER TABLE v2_ephemeris_elements DROP CONSTRAINT v2_ephemeris_elements_target_id_fkey;

ALTER TABLE v2_phase_i_proposals ADD FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_phase_i_proposals ADD FOREIGN KEY (itac_id) REFERENCES v2_itacs(id) ON DELETE CASCADE;
ALTER TABLE v2_phase_i_proposals ADD FOREIGN KEY (exchange_submission_id) REFERENCES v2_submissions(id) ON DELETE CASCADE;

ALTER TABLE v2_blueprints ADD FOREIGN KEY (phase_i_proposal_id) REFERENCES v2_phase_i_proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_conditions ADD FOREIGN KEY  (phase_i_proposal_id) REFERENCES v2_phase_i_proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_keywords ADD FOREIGN KEY (v2_phase_i_proposal_id) REFERENCES v2_phase_i_proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_observations ADD FOREIGN KEY (phase_i_proposal_id) REFERENCES v2_phase_i_proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_phase_i_proposals_classical_investigators ADD FOREIGN KEY (phase_i_proposal_id) REFERENCES v2_phase_i_proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_submissions ADD FOREIGN KEY (phase_i_proposal_id) REFERENCES v2_phase_i_proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_targets ADD FOREIGN KEY (phase_i_proposal_id) REFERENCES v2_phase_i_proposals(id) ON DELETE CASCADE;
ALTER TABLE bandings ADD FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE;
ALTER TABLE v2_proposals_co_investigators ADD FOREIGN KEY (investigator_id) REFERENCES v2_investigators(id) ON DELETE CASCADE;
ALTER TABLE v2_gmoss_filters ADD FOREIGN KEY (blueprint_id) REFERENCES v2_blueprints(id) ON DELETE CASCADE;
ALTER TABLE v2_nici_blue_filters ADD FOREIGN KEY (blueprint_id) REFERENCES v2_blueprints(id) ON DELETE CASCADE;
ALTER TABLE v2_nici_red_filters ADD FOREIGN KEY (blueprint_id) REFERENCES v2_blueprints(id) ON DELETE CASCADE;
ALTER TABLE v2_niri_filters ADD FOREIGN KEY (blueprint_id) REFERENCES v2_blueprints(id) ON DELETE CASCADE;
ALTER TABLE v2_magnitudes ADD FOREIGN KEY (target_id) REFERENCES v2_targets(id) ON DELETE CASCADE;
ALTER TABLE v2_ephemeris_elements ADD FOREIGN KEY (target_id) REFERENCES v2_targets(id) ON DELETE CASCADE;

DELETE FROM proposals p
    WHERE p.partner_id = (SELECT partners.id FROM partners WHERE partners.partner_country_key = 'UK');

DELETE FROM partners  WHERE partner_country_key = 'UK';
