-- Pre-calibrations for Contros pCO2 reducer test

-- 1624838400000 = 2021-06-21 00:00:00

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.F', 1626393600000, '{"Value":"61279"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.Runtime', 1626393600000, '{"Value":"1468557"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.k1', 1626393600000, '{"Value":"6.091845e-02"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.k2', 1626393600000, '{"Value":"3.755323e-06"}', 'CalculationCoefficient');

INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.k3', 1626393600000, '{"Value":"2.965241e-10"}', 'CalculationCoefficient');

  INSERT INTO calibration (instrument_id, type, target, deployment_date, coefficients, class)
  VALUES (124, 'CALC_COEFFICIENT', '6.Response Time', 1626393600000, '{"Value":"175"}', 'CalculationCoefficient');
