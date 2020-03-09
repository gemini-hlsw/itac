create INDEX log_entry_updated_at_index ON log_entries(updated_at);
create index log_note_log_entry_index ON log_notes(log_entry_id);
create index log_entry_types_log_entry_index ON log_entry_types(log_entry_id);

