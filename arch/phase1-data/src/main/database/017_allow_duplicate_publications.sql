alter table p1_publications drop constraint p1_publications_pkey;
alter table p1_publications add column order_nr integer;
alter table p1_publications add primary key (proposal_support_id, text, order_nr);