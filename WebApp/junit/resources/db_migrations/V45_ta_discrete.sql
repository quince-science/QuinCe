-- Sensor types
INSERT INTO sensor_types (
    name, vargroup, display_order, column_code
  ) VALUES (
    'TA [discrete]', 'TA', 10000, 'MDMAP014'
  );

INSERT INTO sensor_types (
    name, vargroup, display_order, column_code
  ) VALUES (
    'Depth', 'TA', 10000, 'ADEPZZ01'
  );

-- Variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('TA [discrete]', NULL, NULL);

-- Marine variable sensors
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'TA [discrete]'),
    (SELECT id FROM sensor_types WHERE name = 'TA [discrete]'),
    1, 0, 0
  );
  
INSERT INTO variable_sensors (
    variable_id, sensor_type, questionable_cascade, bad_cascade
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'TA [discrete]'),
    (SELECT id FROM sensor_types WHERE name = 'Depth'), 0, 0
  );
  
INSERT INTO variable_sensors (
    variable_id, sensor_type, questionable_cascade, bad_cascade
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'TA [discrete]'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'), 0, 0
  );
