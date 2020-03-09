-- We are going to be reversing this back out at some point, but at the moment, the schema does not support a gemini comment.
ALTER TABLE v2_itacs ADD COLUMN gemini_comment VARCHAR DEFAULT 'None';