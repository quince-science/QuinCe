-- Rename the SailDrone variables
UPDATE variables SET name = 'SailDrone Marine CO₂ NRT' WHERE id = 3;
UPDATE variables SET name = 'SailDrone Atmospheric CO₂ NRT' WHERE id = 4;

-- Add LICOR pressures
INSERT INTO sensor_types (id, name, vargroup, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (26, 'LICOR Pressure (Equilibrator)', 'Pressure', 0, 0, 449, 'LPRES0EQ', 'LICOR Pressure (Equilibrator)', 'hPa');
INSERT INTO sensor_types (id, name, vargroup, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (27, 'LICOR Pressure (Atmosphere)', 'Pressure', 0, 0, 450, 'LPRES0EQ', 'LICOR Pressure (Atmosphere)', 'hPa');

-- Replace Equilibrator Pressure with LICOR pressure for marine
UPDATE variable_sensors SET sensor_type = 26 WHERE variable_id = 3 AND sensor_type = 4;

-- Replace Atmospheric Pressure with LICOR pressure for atmos
UPDATE variable_sensors SET sensor_type = 27 WHERE variable_id = 4 AND sensor_type = 10;
