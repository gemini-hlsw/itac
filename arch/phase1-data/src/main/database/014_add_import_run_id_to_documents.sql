ALTER TABLE "public"."p1_documents"
	ADD COLUMN "import_run_id" bigint NULL;
COMMENT ON COLUMN "public"."p1_documents"."import_run_id" IS 'Unique id for each import run. Null for successfully imported documents.' ;