INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Grandparent', 'Group', NULL, NULL, NULL, 0, 0);
  
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Parent', 'Group', (SELECT id FROM sensor_types WHERE name = 'Grandparent'), NULL, NULL, 0, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Aunt', 'Group', (SELECT id FROM sensor_types WHERE name = 'Grandparent'), NULL, NULL, 0, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Child', 'Group', (SELECT id FROM sensor_types WHERE name = 'Parent'), NULL, NULL, 0, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Child 2', 'Group', (SELECT id FROM sensor_types WHERE name = 'Parent'), NULL, NULL, 0, 0);
