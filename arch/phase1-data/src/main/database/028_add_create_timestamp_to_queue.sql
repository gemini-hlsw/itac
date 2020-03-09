ALTER TABLE public.queues
	ADD COLUMN "created_timestamp" timestamp NULL;
COMMENT ON COLUMN public.queues.created_timestamp IS 'Creation timestamp for ordering of queues in display.' ;