-- Second test variable
INSERT INTO variables (id, name, allowed_basis) VALUES (2000000, 'childRequiredVar', 1);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    2000000,
    (SELECT id FROM sensor_types WHERE name = 'xCO₂ (with standards)'),
    1, '{"Time":[[3,3],[4,4]]}'
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, cascades)
  VALUES (
    2000000,
    (SELECT id FROM sensor_types WHERE name = 'Equilibrator Pressure (differential)'),
    0, '{"Time":[[3,3],[4,4]]}'
  );
