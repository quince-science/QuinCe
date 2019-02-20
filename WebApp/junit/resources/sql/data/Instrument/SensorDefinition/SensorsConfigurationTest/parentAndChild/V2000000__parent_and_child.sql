INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Grandparent', NULL, NULL, NULL, 0, 0);
  
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Parent', (SELECT id FROM sensor_types WHERE name = 'Grandparent'), NULL, NULL, 0, 0);

INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Aunt', (SELECT id FROM sensor_types WHERE name = 'Grandparent'), NULL, NULL, 0, 0);

  
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Child', (SELECT id FROM sensor_types WHERE name = 'Parent'), NULL, NULL, 0, 0);

  INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Child 2', (SELECT id FROM sensor_types WHERE name = 'Parent'), NULL, NULL, 0, 0);
