ALTER TABLE "public"."bandings"
	ADD COLUMN "banding_type" text NULL;
ALTER TABLE "public"."bandings"
	ADD COLUMN "joint_banding_id" int NULL;
COMMENT ON COLUMN "public"."bandings"."banding_type" IS 'Discriminator column for Banding / JointBanding inheritance' ;
COMMENT ON COLUMN "public"."bandings"."joint_banding_id" IS 'If present, indicates the owning joint banding of this banding' ;
