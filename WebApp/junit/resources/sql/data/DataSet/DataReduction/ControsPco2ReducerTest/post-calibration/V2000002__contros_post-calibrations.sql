-- Pre-calibrations for Contros pCO2 reducer test

-- 1625961600000 = 2021-07-11 00:00:00

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.Runtime', 1704931200000, '{"Value":"4121874"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.k1', 1704931200000, '{"Value":"6.347129e-02"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.k2', 1704931200000, '{"Value":"2.713467e-06"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.k3', 1704931200000, '{"Value":"3.840130e-10"}', 'CalculationCoefficient');
