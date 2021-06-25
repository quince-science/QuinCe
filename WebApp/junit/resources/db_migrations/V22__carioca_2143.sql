-- Move diagnostic display orders
UPDATE sensor_types SET display_order = 10000 WHERE id = 12;
UPDATE sensor_types SET display_order = 10001 WHERE id = 42;
UPDATE sensor_types SET display_order = 10002 WHERE id = 13;
UPDATE sensor_types SET display_order = 10003 WHERE id = 14;
UPDATE sensor_types SET display_order = 10004 WHERE id = 15;
UPDATE sensor_types SET display_order = 10005 WHERE id = 16;


-- Carioca submersible sensor

-- Sensor Types
INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'Th', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1300,
    'CARTH', 'Th', NULL
  );

INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'Refb', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1301,
    'CARREFB', 'Refb', NULL
  );

INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'Refh', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1302,
    'CARREFH', 'Refh', NULL
  );

INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    '810nm', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1303,
    'CAR810NM', '810nm', NULL
  );

INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    '596nm', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1304,
    'CAR596NM', '596nm', NULL
  );

INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    '434nm', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1305,
    'CAR434NM', '434nm', NULL
  );


-- The variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('Carioca (experimental)', NULL,
    '{"coefficients": ["tempA", "tempB", "tempC", "tempRL", "tempRH", "tempR1", "co2a", "co2b", "co2c", "co2k", "co2k''", "A_T", "e1"]}');

-- Variable Sensors
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Carioca'),
    (SELECT id FROM sensor_types WHERE column_code = 'CARTH'),
    0, 3, 4, NULL, NULL, NULL
  );

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Carioca'),
    (SELECT id FROM sensor_types WHERE column_code = 'CARREFB'),
    0, 3, 4, NULL, NULL, NULL
  );

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Carioca'),
    (SELECT id FROM sensor_types WHERE column_code = 'CARREFH'),
    0, 3, 4, NULL, NULL, NULL
  );

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Carioca'),
    (SELECT id FROM sensor_types WHERE column_code = 'CAR810NM'),
    0, 3, 4, NULL, NULL, NULL
  );

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Carioca'),
    (SELECT id FROM sensor_types WHERE column_code = 'CAR596NM'),
    0, 3, 4, NULL, NULL, NULL
  );

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Carioca'),
    (SELECT id FROM sensor_types WHERE column_code = 'CAR434NM'),
    0, 3, 4, NULL, NULL, NULL
  );

