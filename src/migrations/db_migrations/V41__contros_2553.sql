-- Rename Intake Temperature to Water Temperature (universal for all variables)
UPDATE sensor_types SET name = 'Water Temperature' WHERE name = 'Intake Temperature';

-- Remove unused sensors

-- Membrane temperature
DELETE FROM variable_sensors WHERE variable_id = 6 AND sensor_type = 39;

-- Membrane temperature is no longer used anywhere
DELETE FROM sensor_types WHERE id = 39;

-- Salinity is not used in data reduction, so its cascade should have no effect
UPDATE variable_sensors SET bad_cascade = 2 WHERE variable_id = 6 AND sensor_type = 2;

-- Rename the CONTROS sensor types
UPDATE sensor_types SET name = 'Raw Detector Signal' WHERE name = 'Contros pCO₂ Raw Detector Signal';
UPDATE sensor_types SET name = 'Reference Signal' WHERE name = 'Contros pCO₂ Reference Signal';
UPDATE sensor_types SET name = 'Zero Mode' WHERE name = 'Contros pCO₂ Zero Mode';
UPDATE sensor_types SET name = 'Flush Mode' WHERE name = 'Contros pCO₂ Flush Mode';
UPDATE sensor_types SET name = 'Runtime' WHERE name = 'Contros pCO₂ Runtime';

-- Change required coefficients
UPDATE variables SET properties = '{"coefficients": ["F", "Runtime", "k1", "k2", "k3"]}' WHERE id = 6;

-- Add zero interpolation mode question
UPDATE variables SET attributes =
  '{"zero_mode": {"name": "Measurement mode", "type": "ENUM", "values": ["Continuous", "Zero before sleep", "Zero after sleep"]}, "zero_flush": {"name": "Zero Flushing Time (s)", "type": "NUMBER"}}'
  WHERE id = 6;
  
-- Add Water Temperature as a required sensor
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (6, 1, 0, 3, 4);