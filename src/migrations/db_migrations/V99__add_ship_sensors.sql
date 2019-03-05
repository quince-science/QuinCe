-- Add ship speed and direction sensors
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ship Speed', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ship Course', 'Other', NULL, NULL, NULL, 0);

-- Add wind speed and direction sensors. Speeds can be 
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Speed', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Direction (absolute)', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Direction (relative)', 'Other', NULL, NULL, NULL, 0);
