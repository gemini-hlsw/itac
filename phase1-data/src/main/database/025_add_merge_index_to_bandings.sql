-- ITAC-151 the merge index contains the order that proposals are accepted into the queue.

alter table bandings add column merge_index integer;
