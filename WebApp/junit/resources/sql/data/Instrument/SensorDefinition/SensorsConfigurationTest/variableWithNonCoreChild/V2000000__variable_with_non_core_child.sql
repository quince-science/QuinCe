-- A test variable
INSERT INTO variables (name, allowed_basis) VALUES ('Has Child', 1);

-- A valid core sensor
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    (SELECT id FROM variables WHERE name = 'Has Child'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    1, '{"Time":[[3,3],[4,4]]}'
  );

-- Non-Core sensor that is a child
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    (SELECT id FROM variables WHERE name = 'Has Child'),
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure (absolute)'),
    0, '{"Time":[[3,3],[4,4]]}'
  );
