-- Scanning the postgres statistics turned up some serious sequential scans.
CREATE INDEX v2_phase_i_proposals_proposals_index ON v2_phase_i_proposals(proposal_id);

CREATE INDEX v2_guide_stars_target_index ON v2_guide_stars(observation_id);
CREATE INDEX v2_guide_stars_observation_index ON v2_guide_stars(target_id);

CREATE INDEX v2_magnitudes_target_index ON v2_magnitudes(observation_id);

CREATE INDEX partners_name_index ON partners(name);
CREATE INDEX partners_abbreviation_index ON partners(abbreviation);

CREATE INDEX v2_submissions_phase_i_proposal_index ON v2_submissions(phase_i_proposal_id);
CREATE INDEX v2_submissions_partner_index ON v2_submissions(partner_id);

CREATE INDEX v2_observations_phase_i_proposal_index ON v2_observations(phase_i_proposal_id);
CREATE INDEX v2_observations_target_index ON v2_observations(target_id);
CREATE INDEX v2_observations_blueprint_index ON v2_observations(blueprint_id);
CREATE INDEX v2_observations_condition_index ON v2_observations(condition_id);

CREATE INDEX v2_blueprints_phase_i_proposal_index ON v2_blueprints(phase_i_proposal_id);

CREATE INDEX v2_targets_phase_i_proposal_index ON v2_targets(phase_i_proposal_id);

CREATE INDEX v2_conditions_phase_i_proposal_index ON v2_conditions(phase_i_proposal_id);

CREATE INDEX v2_gmosn_filters_blueprint_index            ON v2_gmosn_filters(blueprint_id);
CREATE INDEX v2_gmoss_filters_blueprint_index            ON v2_gmoss_filters (blueprint_id);
CREATE INDEX v2_nici_red_filters_blueprint_index         ON v2_nici_red_filters(blueprint_id);
CREATE INDEX v2_nici_blue_filters_blueprint_index        ON v2_nici_blue_filters(blueprint_id);
CREATE INDEX v2_michelle_imaging_filters_blueprint_index ON v2_michelle_imaging_filters(blueprint_id);
CREATE INDEX v2_niri_filters_blueprint_index             ON v2_niri_filters(blueprint_id);

CREATE INDEX v2_ephemeris_elements_coordinate_index ON v2_ephemeris_elements(coordinate_id);
CREATE INDEX v2_ephemeris_elements_target_index     ON v2_ephemeris_elements(target_id);


-- Not currently handling.

-- v2_proper_motions                            |     3218
-- partner_charges                              |     5068
-- v2_investigators                             |     4480
-- v2_itacs                                     |     2107
-- conditions                                   |     3917
-- proposal_issues                              |     2037
-- v2_phase_i_proposals_classical_investigators |     1981
-- people                                       |     1914
-- bandings                                     |    13522
-- queue_proposals_classical                    |    14035
