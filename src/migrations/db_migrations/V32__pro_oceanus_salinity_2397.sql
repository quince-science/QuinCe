-- Add Salinity requirement to Pro Oceanus
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PSALPR01'),
    0, 3, 4, NULL, NULL, NULL
  );
