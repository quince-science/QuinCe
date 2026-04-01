-- A test variable
INSERT INTO variables (name, allowed_basis) VALUES ('Has Child', 1);

-- Core sensor that is a child
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    (SELECT id FROM variables WHERE name = 'Has Child'),
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure (absolute)'),
    1, '{"Time":[[3,3],[4,4]]}'
  );
