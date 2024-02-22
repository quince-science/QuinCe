-- Provides a basis for tests that require data reduction to run.
-- Independent of all other entries in the testbase folder

-- Not intended for use in testing actual data reduction routines,
-- but for the general mechanics of data reduction.

-- This creates a base Variable with required SensorTypes
-- and an Instrument that uses that Variable, plus an empty DataSet.
-- Migrations in sub-folders will create SensorValues and
-- Measurements, and may override some of the Instrument/Variable
-- settings.

-- -----------------------------------------------------------------------

-- A user
INSERT INTO user (id, email, firstname, surname, salt, password)
  VALUES (1, 'test@test.com', 'Fred', 'Bloggs', '', '');

-- A variable. Uses SST, Salinity and CO2
INSERT INTO variables (id, name) VALUES (1000000, 'testVar');

-- Sensors for the variable. No flags cascade
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Intake Temperature'),
    0, 2, 2
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    0, 2, 2
  );

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (
    (SELECT id FROM variables WHERE name = 'testVar'),
    (SELECT id FROM sensor_types WHERE name = 'xCO₂ (wet, no standards)'),
    1, 2, 2
  );

  
-- Instrument
INSERT INTO instrument
  (id, owner, name, platform_name, platform_code, nrt, properties)
  VALUES (1, 1, 'Instrument', 'Platform', 'CODE' ,0 , '{"latitude":"0","longitude":"0"}');
  
INSERT INTO instrument_variables (instrument_id, variable_id)
  VALUES (1, 1000000);

-- File definition.
-- This isn't actually used, but we need a file definition and file columns
-- to conform to the daINSERT INTO file_definition VALUES
INSERT INTO file_definition
  (id, instrument_id, description, column_separator, header_type, header_lines,
   header_end_string, column_header_rows, column_count, lon_spec, lat_spec,
   datetime_spec)
  VALUES
  (1, 1, 'Data File', ',', 0, 0, NULL, 0, 6,
   '{"valueColumn":-1,"hemisphereColumn":-1,"format":-1}',
   '{"valueColumn":-1,"hemisphereColumn":-1,"format":-1}',
   '{"assignments":{"0":{"assignmentIndex":0,"column":0,"properties":{"formatString":"yyyy-MM-dd HH:mm:ss.SSS"}},"1":{"assignmentIndex":1,"column":-1,"properties":{}},"2":{"assignmentIndex":2,"column":-1,"properties":{}},"3":{"assignmentIndex":3,"column":-1,"properties":{}},"4":{"assignmentIndex":4,"column":-1,"properties":{}},"5":{"assignmentIndex":5,"column":-1,"properties":{}},"6":{"assignmentIndex":6,"column":-1,"properties":{}},"7":{"assignmentIndex":7,"column":-1,"properties":{}},"8":{"assignmentIndex":8,"column":-1,"properties":{}},"9":{"assignmentIndex":9,"column":-1,"properties":{}},"10":{"assignmentIndex":10,"column":-1,"properties":{}},"11":{"assignmentIndex":11,"column":-1,"properties":{}},"12":{"assignmentIndex":12,"column":-1,"properties":{}}},"fileHasHeader":false}'
  );
  
-- Run Types
-- Two different run types refer to our variable - the other is a calibration
-- The calibration is used for diagnostic flagging tests (no calibration performed)
INSERT INTO run_type
  (file_definition_id, run_name, category_code)
  VALUES (1, 'var_1', 1000000);

INSERT INTO run_type
  (file_definition_id, run_name, category_code)
  VALUES (1, 'var_2', 1000000);

INSERT INTO run_type
  (file_definition_id, run_name, category_code)
  VALUES (1, 'calib', -3);

-- File columns
INSERT INTO file_column
  (id, file_definition_id, file_column, primary_sensor, sensor_type, sensor_name)
  VALUES (1, 1, 3, 1, -1, 'Run Type');

INSERT INTO file_column
  (id, file_definition_id, file_column, primary_sensor, sensor_type, sensor_name)
  VALUES
  (2, 1, 3, 1,
   (SELECT id FROM sensor_types WHERE name = 'Intake Temperature'),
   'SST');

INSERT INTO file_column
  (id, file_definition_id, file_column, primary_sensor, sensor_type, sensor_name)
  VALUES
  (3, 1, 4, 1,
   (SELECT id FROM sensor_types WHERE name = 'Salinity'),
   'SAL');

INSERT INTO file_column
  (id, file_definition_id, file_column, primary_sensor, sensor_type, sensor_name)
  VALUES
  (4, 1, 5, 1,
   (SELECT id FROM sensor_types WHERE name = 'xCO₂ (wet, no standards)'),
   'CO2');
   
-- Two diagnostic sensors
INSERT INTO file_column
  (id, file_definition_id, file_column, primary_sensor, sensor_type, sensor_name)
  VALUES
  (5, 1, 6, 1,
   (SELECT id FROM sensor_types WHERE name = 'Diagnostic Water Flow'),
   'Water Flow');

INSERT INTO file_column
  (id, file_definition_id, file_column, primary_sensor, sensor_type, sensor_name)
  VALUES
  (6, 1, 7, 1,
   (SELECT id FROM sensor_types WHERE name = 'Diagnostic Gas Flow'),
   'Air Flow');



 -- A dataset.
 -- Start date = 2024-01-01 00:00:00
 -- End date = 2024-01-10 00:00:00
 INSERT INTO dataset
   (id, instrument_id, name, start, end, min_longitude, max_longitude, min_latitude, max_latitude,
    status, status_date, properties)
   VALUES
   (1, 1, 'Dataset', 1704067200000, 1704844800000, 0, 0, 0, 0, 50, 1704844800000,
   '{"_INSTRUMENT":{"depth":"2","latitude":"0","postFlushingTime":"0","preFlushingTime":"0","longitude":"0"},"_DATASET":{"ProcessingVersion":"v19.2.15"},"__SENSOR_OFFSETS":{}}');