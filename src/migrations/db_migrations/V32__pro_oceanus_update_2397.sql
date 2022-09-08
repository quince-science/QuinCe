-- Add the Pro Oceanus Current Count.
-- Not used for calculations, but can be useful for QC
INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'ProOceanus Current Count', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1255,
    'PROCO2CURR', 'Zero Count', NULL
  );

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PROCO2CURR'),
    0, 3, 4, NULL, NULL, NULL
  );

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'PROCO2CURR'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Add Salinity requirement to Pro Oceanus. Not used in calculations,
-- but it should still be recorded as a matter of course.
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PSALPR01'),
    0, 2, 2, NULL, NULL, NULL
  );
