-- Japan custom variable. Has slope and intercept.
-- This is a temporary variable for the intercomparison. It should not be
-- used long-term, as the slope and intercept should be calculated from the
-- data.

-- Measurement Index
INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'Measurement Index', 'Other', NULL, NULL, NULL, 0, 0, 9000,
    'INDEX', 'Measurement Index', NULL
  );


-- The variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('Japan Custom (temp)', NULL,
    '{"coefficients": ["Base Slope", "Base Intercept", "Slope Adjustment", "Intercept Adjustment"]}');

-- Variable sensors

-- SST (may be the same as EqT)
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Japan Custom (temp)'),
    (SELECT id FROM sensor_types WHERE column_code = 'INDEX'),
    0, 3, 4, NULL, NULL, NULL
  );

-- SST (may be the same as EqT)
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Japan Custom (temp)'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPPR01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- EqT
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Japan Custom (temp)'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPEQMN'),
    0, 3, 4, NULL, NULL, NULL
  );

-- pCO₂
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Japan Custom (temp)'),
    (SELECT id FROM sensor_types WHERE column_code = 'PCO2IG02'),
    1, 3, 4, NULL, NULL, NULL
  );
