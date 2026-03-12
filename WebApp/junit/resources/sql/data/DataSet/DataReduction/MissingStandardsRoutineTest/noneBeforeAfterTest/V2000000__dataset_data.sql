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
-- None

-- Measured value
INSERT INTO coordinates(id, dataset_id, date)
  VALUES (4, 1, 1609502400000);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (7, 4, 5, 350, 2);
INSERT INTO sensor_values(id, coordinate_id, file_column, value, user_qc_flag)
  VALUES (8, 4, 6, 'equ', 2);
  
-- Subsequent internal calibration value
-- None

-- Measurements
INSERT INTO measurements (id, coordinate_id, measurement_values)
  VALUES (4, 4, '{"9":{"svids":[7],"suppids":[],"memberCount":1,"interpolatesOverFlag":false,"value":402.41366693446776,"flag":-2,"qcComments":[],"type":"M","props":{}}}');

-- Measurement run types
INSERT INTO measurement_run_types (measurement_id, variable_id, run_type)
  VALUES (4, 1, 'equ');

-- Data Reduction record
INSERT INTO data_reduction (measurement_id, variable_id, calculation_values, qc_flag)
  VALUES (4, 1, '{}', 2);
