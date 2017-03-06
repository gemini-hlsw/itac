-- Due to a software bug (ITAC-650) some v2_itacs were set to a bogus default value
-- The command below resets them to 0
UPDATE schema_version SET version = 83;

update v2_itacs set accept_program_id=null, accept_contact=null, accept_email=null, accept_band=null, accept_award_value=null, accept_award_units=null, accept_rollover=null where accept_band = 0 or accept_band is null;