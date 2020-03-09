UPDATE schema_version SET version = 72;

ALTER TABLE queues ADD COLUMN subaru_scheduled_by_queue_engine boolean DEFAULT FALSE;

DROP TABLE IF EXISTS partner_percentage CASCADE;

CREATE TABLE partner_percentage (
  id integer not null,
  queue_id integer not null,
  partner_id integer not null,
  percentage real not null default 0.0
);

ALTER TABLE partner_percentage ADD PRIMARY KEY (id);

CREATE INDEX partner_percentage_queue_id_index on partner_percentage(queue_id);

ALTER TABLE partners ALTER COLUMN percentage_share TYPE real;
