-- Add field for last NRT export time
ALTER TABLE instrument ADD COLUMN last_nrt_export BIGINT NULL DEFAULT NULL AFTER `nrt`;
