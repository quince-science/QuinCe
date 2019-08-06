INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order)
  VALUES ('Grandparent', 'Group', NULL, NULL, NULL, 0, 0, 1000);
  
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order)
  VALUES ('Parent', 'Group', (SELECT id FROM sensor_types WHERE name = 'Grandparent'), NULL, NULL, 0, 0, 1001);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order)
  VALUES ('Aunt', 'Group', (SELECT id FROM sensor_types WHERE name = 'Grandparent'), NULL, NULL, 0, 0, 1002);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order)
  VALUES ('Child', 'Group', (SELECT id FROM sensor_types WHERE name = 'Parent'), NULL, NULL, 0, 0, 1003);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order)
  VALUES ('Child 2', 'Group', (SELECT id FROM sensor_types WHERE name = 'Parent'), NULL, NULL, 0, 0, 1004);
