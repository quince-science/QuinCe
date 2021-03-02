-- Add measurement_values column to measurements
ALTER TABLE measurements ADD COLUMN measurement_values MEDIUMTEXT NULL;

-- Create customisable column headers for SensorTypes in different variables
-- H2 (test database) can't add multiple columns in one statement
ALTER TABLE variable_sensors ADD COLUMN export_column_short VARCHAR(100) NULL AFTER bad_cascade;
ALTER TABLE variable_sensors ADD COLUMN export_column_long VARCHAR(100) NULL AFTER export_column_short;
ALTER TABLE variable_sensors ADD COLUMN export_column_code VARCHAR(50) NULL AFTER export_column_long;

-- Update incorrect column info
UPDATE sensor_types SET column_code = 'XCO2WBDY' WHERE id=9;

-- Add custom columns for Underway pCO2 systems
UPDATE variable_sensors SET
  export_column_short = 'xCO₂ in Water',
  export_column_long = 'xCO₂ in Water - Calibrated',
  export_column_code = 'XCO2DCEQ'
  WHERE variable_id = 1 AND sensor_type = 9;

UPDATE variable_sensors SET
  export_column_short = 'xCO₂ in Atmosphere',
  export_column_long = 'xCO₂ in Atmosphere - Calibrated',
  export_column_code = 'XCO2DCMA'
  WHERE variable_id = 2 AND sensor_type = 9;


-- Store all the current dataset statuses in a temporary table
DROP TABLE IF EXISTS v12_dataset_statuses;
CREATE TABLE v12_dataset_statuses AS
  SELECT id, status FROM dataset;


-- Remove all existing measurement info
DELETE FROM data_reduction;
DELETE FROM measurement_values;
DELETE FROM measurements;

-- Set status for all datasets to WAITING
UPDATE dataset SET status = 0;

-- Create Auto QC Jobs for all datasets
-- Use the first owner
DELETE FROM job;

INSERT INTO job (owner, class, properties, status)
SELECT
  (SELECT id FROM user LIMIT 1) AS owner,
  'uk.ac.exeter.QuinCe.jobs.files.AutoQCJob' AS class,
  CONCAT('{"id":"', dataset.id, '"}') AS properties,
  'WAITING' AS status
FROM dataset;
