-- Add field to track if a dataset has been exported
ALTER TABLE dataset ADD COLUMN exported TINYINT NOT NULL DEFAULT 0 AFTER last_touched;

-- Set all existing datasets to exported = 1 (safest option)
UPDATE dataset SET exported = 1;
