-- Fix Membrane Temperautre
UPDATE sensor_types SET vargroup = 'Temperature' WHERE name = 'Membrane Temperature';

-- Add 'Cell Gas Pressure'
INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Cell Gas Pressure', 'Pressure', 452, 'CELGASPRES', 'Pressure in Gas Cell', 'mbar');

-- Replace Membrane Pressure with Cell Gas Pressure in variable definition
UPDATE variable_sensors SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Cell Gas Pressure') WHERE
  variable_id = (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere') AND
  sensor_type = (SELECT id FROM sensor_types WHERE name = 'Membrane Pressure');

UPDATE variable_sensors SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Cell Gas Pressure') WHERE
  variable_id = (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water') AND
  sensor_type = (SELECT id FROM sensor_types WHERE name = 'Membrane Pressure');


-- Update all instruments
UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Cell Gas Pressure') WHERE
  sensor_type = (SELECT id FROM sensor_types WHERE name = 'Membrane Pressure') AND
  file_definition_id IN
    (SELECT id FROM file_definition WHERE instrument_id IN
      (
        (SELECT instrument_id FROM instrument_variables WHERE variable_id =
          (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere')
        )
      )
    );

UPDATE file_column SET sensor_type = (SELECT id FROM sensor_types WHERE name = 'Cell Gas Pressure') WHERE
  sensor_type = (SELECT id FROM sensor_types WHERE name = 'Membrane Pressure') AND
  file_definition_id IN
    (SELECT id FROM file_definition WHERE instrument_id IN
      (
        (SELECT instrument_id FROM instrument_variables WHERE variable_id =
          (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water')
        )
      )
    );

-- Add Humidity Pressure sensor type
INSERT INTO sensor_types (name, vargroup, display_order, column_code, column_heading, units)
  VALUES ('Humidity Pressure', 'Pressure', 453, 'HUMPRES', 'Humidity Pressure', 'mbar');

INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE name = 'Humidity Pressure'),
    0, 3, 4
  );
  
-- Rename xCO2 sensor titles
UPDATE variable_sensors SET export_column_short = 'xCO₂ atm'
  WHERE export_column_short = 'xCO₂ in Atmosphere';
UPDATE variable_sensors SET export_column_long = 'xCO₂ atm'
  WHERE export_column_long = 'xCO₂ in Atmosphere';
UPDATE variable_sensors SET export_column_long = 'xCO₂ atm calibrated'
  WHERE export_column_long = 'xCO₂ in Atmosphere - Calibrated';
  
UPDATE variable_sensors SET export_column_short = 'xCO₂ water'
  WHERE export_column_short = 'xCO₂ in Water';
UPDATE variable_sensors SET export_column_long = 'xCO₂ water'
  WHERE export_column_long = 'xCO₂ in Water';
UPDATE variable_sensors SET export_column_long = 'xCO₂ water calibrated'
  WHERE export_column_long = 'xCO₂ in Water - Calibrated';
