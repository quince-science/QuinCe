# Add column to the run types table to support aliases
ALTER TABLE `run_type` ADD `alias_to` VARCHAR(50) NULL DEFAULT NULL AFTER `category_code`;
