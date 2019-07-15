-- Add attributes to variables, used to ask general questions about each
-- variable when an instrument is created. Each attribute is given a name
-- (used internally) and a description (for display to the user).
-- Attributes are stored as JSON with the following format:
--
-- {
--   "atm_pres_sensor_height": "Atmospheric Pressure Sensor Height",
--   "intake_height": "Air Intake Height"
-- }
--
-- The attributes will be specified for each variable attached to an instrument,
-- again using JSON:
--
-- {
--   "atm_pres_sensor_height": 10,
--   "intake_height": 15
-- }
--
--
-- Note that Underway Marine pCO2 does not have any 

-- Add attributes field for variables
ALTER TABLE variables ADD attributes MEDIUMTEXT NULL AFTER name;

-- Add attributes field to instrument_variables
ALTER TABLE instrument_variables ADD attributes MEDIUMTEXT NULL AFTER variable_id;

-- Add Atmospheric CO₂ variable
INSERT INTO variables (id, name, attributes)
  VALUES (2, 'Underway Atmospheric pCO₂',
          '{"atm_pres_sensor_height": "Atmospheric Pressure Sensor Height"}');

-- Sensors required for Atmospheric CO₂

-- Equilibrator Temperature          
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (2, 3, 0, 3, 4);

-- Salinity
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (2, 2, 0, 2, 3);

-- Atmospheric Pressure
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (2, 10, 0, 3, 4);

-- CO₂ in gas
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade) VALUES (2, 9, 1, 3, 4);
