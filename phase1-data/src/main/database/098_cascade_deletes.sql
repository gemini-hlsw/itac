UPDATE schema_version SET version = 98;

alter table log_entries drop constraint log_entries_proposal_id_fkey, add constraint log_entries_proposal_id_fkey FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE DEFERRABLE;
alter table v2_flamingos2_filters drop constraint v2_flamingos2_filters_blueprint_id_fkey, add constraint log_entries_proposal_id_fkey FOREIGN KEY (blueprint_id) REFERENCES v2_blueprints(id) ON DELETE CASCADE DEFERRABLE;

