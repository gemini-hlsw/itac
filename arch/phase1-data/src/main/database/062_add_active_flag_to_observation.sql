-- UX-1348 Observation select active/inactive. Queue aware.
ALTER TABLE v2_observations ADD COLUMN active BOOLEAN DEFAULT true;