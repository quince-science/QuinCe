-- Run Types

-- Run Type values
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'std1', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'std2', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'std3', -3);
INSERT INTO run_type (file_definition_id, run_name, category_code)
  VALUES (1, 'equ', 1);

-- Coordinates and Sensor Values

-- Preceding internal calibration value
INSERT INTO coordinates(id, dataset_id, date)
  VALUES (1, 1, 1609498800000);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (1, 1, 5, 200, 2);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (2, 1, 6, 'std1', 2);

INSERT INTO coordinates(id, dataset_id, date)
  VALUES (2, 1, 1609498801000);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (3, 2, 5, 400, 2);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (4, 2, 6, 'std2', 2);

INSERT INTO coordinates(id, dataset_id, date)
  VALUES (3, 1, 1609498802000);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (5, 3, 5, 800, 2);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (6, 3, 6, 'std3', 2);
  
-- Measured value
INSERT INTO coordinates(id, dataset_id, date)
  VALUES (4, 1, 1609502400000);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (7, 4, 5, 350, 2);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (8, 4, 6, 'equ', 2);
  
-- Subsequent internal calibration value
INSERT INTO coordinates(id, dataset_id, date)
  VALUES (5, 1, 1609506000000);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (9, 5, 5, 200, 2);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (10, 5, 6, 'std1', 2);

-- Measurements
INSERT INTO measurements (id, coordinate_id, measurement_values)
  VALUES (1, 1, NULL);
INSERT INTO measurements (id, coordinate_id, measurement_values)
  VALUES (2, 2, NULL);
INSERT INTO measurements (id, coordinate_id, measurement_values)
  VALUES (3, 3, NULL);
INSERT INTO measurements (id, coordinate_id, measurement_values)
  VALUES (4, 4, '{"9":{"svids":[7],"suppids":[1, 3, 5, 9],"memberCount":1,"interpolatesOverFlag":false,"value":402.41366693446776,"flag":-2,"qcComments":[],"type":"M","props":{}}}');
INSERT INTO measurements (id, coordinate_id, measurement_values)
  VALUES (5, 5, NULL);

-- Measurement run types
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (1, 1, 'std1');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (2, 1, 'std2');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (3, 1, 'std3');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (4, 1, 'equ');
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (5, 1, 'std1');

-- Data Reduction record
INSERT INTO data_reduction (measurement_id, variable_id, calculation_values, qc_flag)
  VALUES (4, 1, '{}', 2);
