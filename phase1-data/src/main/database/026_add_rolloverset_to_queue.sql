ALTER TABLE public.queues
	ADD COLUMN "rolloverset_id" bigint NULL;
COMMENT ON COLUMN public.queues.rolloverset_id IS 'Unique id for RolloverSet. If null, no RolloverSet' ;