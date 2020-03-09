-- add a flag for finalization to queues
ALTER TABLE queues ADD COLUMN finalized bool;