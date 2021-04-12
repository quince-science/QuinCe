-- Add a properties field to data_file
ALTER TABLE data_file ADD COLUMN properties MEDIUMTEXT NULL AFTER `record_count`;

-- Add default properties
UPDATE data_file SET properties = '{"timeOffset": "0"}';