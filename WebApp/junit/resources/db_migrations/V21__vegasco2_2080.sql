-- Allow long platform codes
ALTER TABLE instrument
  CHANGE COLUMN platform_code platform_code VARCHAR(50) NULL DEFAULT NULL ;

-- Vegas CO2 sensor
    
-- Variable
INSERT INTO variables (name, attributes, properties)
  VALUES ('Vegas CO₂', NULL, NULL);

-- Variable sensors

-- SST (may be the same as EqT)
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Vegas CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPPR01'),
    0, 3, 4, NULL, NULL, NULL
  );
  
-- EqT
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Vegas CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPEQMN'),
    0, 3, 4, NULL, NULL, NULL
  );

-- pCO₂
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Vegas CO₂'),
    (SELECT id FROM sensor_types WHERE column_code = 'PCO2IG02'),
    1, 3, 4, NULL, NULL, NULL
  );
