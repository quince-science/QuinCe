-- Pre-calibrations for Contros pCO2 reducer test

-- 1625961600000 = 2021-07-11 00:00:00

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.Runtime', 1625961600000, '{"Value":"1468557"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.k1', 1625961600000, '{"Value":"6.050551e-02"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.k2', 1625961600000, '{"Value":"3.729867e-06"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (116, 'CALC_COEFFICIENT', '6.k3', 1625961600000, '{"Value":"2.945140e-10"}', 'CalculationCoefficient');
