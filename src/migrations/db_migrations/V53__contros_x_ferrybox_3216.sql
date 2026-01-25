-- Variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('CONTROS pCO₂ via FerryBox',
    '{"zero_mode": {"name": "Measurement mode", "type": "ENUM", "values": ["Continuous", "Zero before sleep", "Zero after sleep"]}, "zero_flush": {"name": "Zero Flushing Time (s)", "type": "NUMBER"}}',
    '{"coefficients": ["F", "Tsensor", "f(Tsensor)", "k1", "k2", "k3", "Runtime", "S''2beam,Z", "Response Time"]}');

-- Variable Sensors (copy from original CONTROS variable)
INSERT INTO variable_sensors (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  SELECT (SELECT id FROM variables WHERE name = 'CONTROS pCO₂ via FerryBox'), sensor_type, core, questionable_cascade, bad_cascade
  FROM variable_sensors WHERE variable_id = 6;
