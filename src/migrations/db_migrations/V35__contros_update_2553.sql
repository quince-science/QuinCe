-- Remove unused sensors

-- Membrane temperature
DELETE FROM variable_sensors WHERE variable_id = 6 AND sensor_type = 39;

-- Membrane temperature is no longer used anywhere
DELETE FROM sensor_types WHERE id = 39;


-- SST and Salinity is not used in data reduction, so its cascade should have no effect
UPDATE variable_sensors SET questionable_cascade = 2 WHERE variable_id = 6 AND sensor_type = 1;
UPDATE variable_sensors SET bad_cascade = 2 WHERE variable_id = 6 AND sensor_type = 1;
UPDATE variable_sensors SET bad_cascade = 2 WHERE variable_id = 6 AND sensor_type = 2;