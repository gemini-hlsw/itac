UPDATE schema_version SET version = 87;

update v2_blueprints set disperser='D_32_LMM' where disperser='LM_32_echelle';
update v2_blueprints set disperser='D_75_LMM' where disperser='LM_75_grating';