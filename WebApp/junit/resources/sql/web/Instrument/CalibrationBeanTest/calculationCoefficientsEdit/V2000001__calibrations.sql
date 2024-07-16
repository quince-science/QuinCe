-- Replace the Variable - we use a different one to the 'standard' setup
UPDATE instrument_variables SET variable_id = 6 WHERE instrument_id = 1;

-- Replace the file columns
DELETE FROM file_column WHERE file_definition_id = 1;

INSERT INTO file_column VALUES (100,1,16,1,16,'P_pump',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (101,1,40,1,40,'p_NDIR',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (102,1,41,1,41,'p_in',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (103,1,15,1,15,'U_supply',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (104,1,35,1,35,'Zero',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (105,1,36,1,36,'Flush',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (106,1,37,1,37,'Runtime',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (107,1,33,1,33,'Signal_raw',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (108,1,34,1,34,'Signal_ref',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (109,1,13,1,13,'T_sensor',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (110,1,11,1,11,'T_control',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (111,1,1638,1,38,'T_gas',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (112,1,42,1,42,'%rH_gas',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (113,1,1,1,1,'TEMP',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');
INSERT INTO file_column VALUES (114,1,2,1,2,'SAL',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');

-- Calculation Coefficients

-- Fixed coefficients

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (100, 1, 'CALC_COEFFICIENT', '6.k1', 1675209600000, '{"Value":"4.976303e-02"}', 'CalculationCoefficient');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (101, 1, 'CALC_COEFFICIENT', '6.k2', 1675209600000, '{"Value":"2.799624e-06"}', 'CalculationCoefficient');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (102, 1, 'CALC_COEFFICIENT', '6.k3', 1675209600000, '{"Value":"1.307428e-10"}', 'CalculationCoefficient');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (103, 1, 'CALC_COEFFICIENT', '6.Response Time', 1675209600000, '{"Value":"65"}', 'CalculationCoefficient');


-- 2023-02-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (1, 1, 'CALC_COEFFICIENT', '6.F', 1675209600000, '{"Value":"65001"}', 'CalculationCoefficient');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (2, 1, 'CALC_COEFFICIENT', '6.Runtime', 1675209600000, '{"Value":"50"}', 'CalculationCoefficient');

-- 2023-05-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (3, 1, 'CALC_COEFFICIENT', '6.Runtime', 1682899200000, '{"Value":"65002"}', 'CalculationCoefficient');

-- 2023-07-01
INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (4, 1, 'CALC_COEFFICIENT', '6.F', 1688169600000, '{"Value":"51"}', 'CalculationCoefficient');

INSERT INTO calibration
  (id, instrument_id, type, target, deployment_date, coefficients, class)
  VALUES
  (5, 1, 'CALC_COEFFICIENT', '6.Runtime', 1688169600000, '{"Value":"52"}', 'CalculationCoefficient');
