-- A test variable
INSERT INTO variables (name, allowed_basis) VALUES ('No Internal Calibration', 1);

-- A valid core sensor
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    (SELECT id FROM variables WHERE name = 'No Internal Calibration'),
    (SELECT id FROM sensor_types WHERE name = 'Water Temperature'),
    1, '{"Time":[[3,3],[4,4]]}'
  );
