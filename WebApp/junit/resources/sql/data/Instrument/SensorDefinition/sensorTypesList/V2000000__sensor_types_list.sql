-- Remove current configuration
DELETE FROM variable_sensors;
DELETE FROM sensor_types;

-- Add unsorted sensor types
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('AAA', NULL, NULL, NULL, 0, 0);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('BBB', NULL, NULL, NULL, 0, 0);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('CCC', NULL, NULL, NULL, 0, 0);
INSERT INTO sensor_types (name, parent, depends_on, depends_question, internal_calibration, diagnostic)
  VALUES ('DDD', NULL, NULL, NULL, 0, 0);
