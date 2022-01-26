-- Each variable must have exactly one core sensor type

-- Soderman
UPDATE variable_sensors SET core = 1 WHERE variable_id = 5 AND sensor_type = 29;

-- CONTROS pCO2 had two core sensor types
UPDATE variable_sensors SET core = 0 WHERE variable_id = 6 AND sensor_type = 34;

-- Carioca
UPDATE variable_sensors SET core = 1 WHERE variable_id = 14 AND sensor_type = 48;
