ALTER TABLE "public"."proposals"
	ADD COLUMN "proposal_type" text NULL;
ALTER TABLE "public"."proposals"
	ADD COLUMN "joint_proposal_id" int NULL;
COMMENT ON COLUMN "public"."proposals"."proposal_type" IS 'Discriminator column for Proposal / JointProposal inheritance' ;
COMMENT ON COLUMN "public"."proposals"."joint_proposal_id" IS 'If present, indicates the owning JP of this proposal' ;
