-- Add a pressure sensor for the SAMI pCO2 sensor
-- Any pressure will do
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'SAMI CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'PRESAMB'),
    0, 2, 2, NULL, NULL, NULL
  );
