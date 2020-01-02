-- Reorganise display orders to add gaps for future sensor types
UPDATE sensor_types SET display_order = 100 WHERE id = 1;
UPDATE sensor_types SET display_order = 200 WHERE id = 2;
UPDATE sensor_types SET display_order = 300 WHERE id = 3;
UPDATE sensor_types SET display_order = 400 WHERE id = 4;
UPDATE sensor_types SET display_order = 400 WHERE id = 5;
UPDATE sensor_types SET display_order = 400 WHERE id = 6;
UPDATE sensor_types SET display_order = 500 WHERE id = 7;
UPDATE sensor_types SET display_order = 600 WHERE id = 8;
UPDATE sensor_types SET display_order = 700 WHERE id = 9;
UPDATE sensor_types SET display_order = 800 WHERE id = 10;
UPDATE sensor_types SET display_order = 900 WHERE id = 11;
UPDATE sensor_types SET display_order = 1300 WHERE id = 12;
UPDATE sensor_types SET display_order = 1400 WHERE id = 13;
UPDATE sensor_types SET display_order = 1500 WHERE id = 14;
UPDATE sensor_types SET display_order = 1600 WHERE id = 15;
UPDATE sensor_types SET display_order = 1700 WHERE id = 16;
UPDATE sensor_types SET display_order = 1800 WHERE id = 17;
UPDATE sensor_types SET display_order = 1900 WHERE id = 18;
UPDATE sensor_types SET display_order = 2000 WHERE id = 19;
UPDATE sensor_types SET display_order = 2100 WHERE id = 20;
UPDATE sensor_types SET display_order = 2200 WHERE id = 21;
UPDATE sensor_types SET display_order = 2300 WHERE id = 22;

-- Rename sensor types
UPDATE sensor_types SET name = "xH₂O (with standards)" WHERE id = 8;
UPDATE sensor_types SET name = "xCO₂ (with standards)" WHERE id = 9;

-- Variable setup for SailDrone CO₂
INSERT INTO variables (id, name, attributes)
  VALUES (3, 'SailDrone Marine CO₂', NULL);

INSERT INTO variables (id, name, attributes)
  VALUES (4, 'SailDrone Atmospheric CO₂', NULL);

-- Air temperature sensor type
INSERT INTO sensor_types (id, name, vargroup, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (23, 'Air Temperature', 'Temperature', 0, 0, 1200, 'CTMPZZ01', 'Temperature of the atmosphere', '°C');

-- New CO₂ sensor types
INSERT INTO sensor_types (id, name, vargroup, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (24, 'xCO₂ water (dry, no standards)', 'CO₂', 0, 0, 1000, 'XCO2WBDY', 'CO₂ Mole Fraction in Water', 'μmol mol-1');

INSERT INTO sensor_types (id, name, vargroup, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (25, 'xCO₂ atmosphere (dry, no standards)', 'CO₂', 0, 0, 1100, 'XCO2DRAT', 'CO₂ Mole Fraction in Atmosphere', 'μmol mol-1');


--
-- Marine CO₂ sensors

-- Intake Temperature
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (3, 1, 0, 3, 4);

-- Salinity
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (3, 2, 0, 2, 3);

-- Equilibrator Pressure
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (3, 4, 0, 2, 3);

-- xCO₂ water
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (3, 24, 1, 3, 4);

--
-- Atmospheric CO₂ sensors

-- Air Temperature
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (4, 23, 0, 3, 4);

-- Salinity
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (4, 2, 0, 2, 3);

-- Atmospheric Pressure
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (4, 10, 0, 3, 4);

-- xCO₂ atmosphere
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (4, 25, 1, 3, 4);
