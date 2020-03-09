-- ITAC-280 Joint proposals can be partially accepted in bands 1-3 and poor weather both.  Constraint is wrong.
ALTER TABLE bandings DROP CONSTRAINT bandings_equality_key;
