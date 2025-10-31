-- Add salinity for the SAMI CO2 sensor.
-- It's not strictly needed for the calculations, but we need to publish it anyway.
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'SAMI CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'PSALPR01'),
    0, 2, 2, NULL, NULL, NULL
  );
