-- ITAC 285 : since joints can be split and therefore banded twice we can not store the awarded time and the
-- program id in the itacExtension of the joint but must store it in the banding and then use those values
-- during queue creation. The awarded time could be calculated on the fly but the program id is a bit more
-- complicated.
ALTER TABLE bandings ADD COLUMN awarded_time_id INTEGER;
ALTER TABLE bandings ADD COLUMN program_id TEXT;

