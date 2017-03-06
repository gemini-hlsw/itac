-- ITAC 358: We need the ability to mark queues as dirty if we do any operations that change the data
-- that was used during queue creation. As a quick fix for issue 358 we will delete bandings that refer
-- to proposals that need to be deleted. This for sure brings the queue in a pretty bad state...
ALTER TABLE queues ADD COLUMN dirty bool DEFAULT false;
