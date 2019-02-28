INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Depends On Me', 'Group', NULL, NULL, NULL, 0, 0);

  INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('Dummy Dependent', 'Group', NULL, (SELECT id FROM sensor_types WHERE name = 'Depends On Me'), NULL, 0, 0);


