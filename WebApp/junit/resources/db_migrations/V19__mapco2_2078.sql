-- MapCO2/ASVCO2 variables

-- Add the MapCo2 variables
INSERT INTO variables (name, attributes, properties)
  VALUES ('MapCO₂ Water', NULL, '{"runType": "EQU"}');
  
INSERT INTO variables (name, attributes, properties)
  VALUES ('MapCO₂ Atmosphere', NULL, '{"runType": "AIR"}');

-- Water sensor types

-- SST
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPPR01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Salinity
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PSALPR01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Equilibrator temperature
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPEQMN'),
    0, 3, 4, NULL, NULL, NULL
  );

  
-- Equilibrator Pressure
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PRESSEQ'),
    0, 3, 4, NULL, NULL, NULL
  );

-- xCO2  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Water'),
    (SELECT id FROM sensor_types WHERE name = 'xCO₂ water (dry, no standards)'),
    1, 3, 4, 'xCO₂ in Water', 'xCO₂ in Water', 'XCO2WBDY'
  );


-- Atmosphere sensor types
-- Atmospheric pressure
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'CAPHZZ01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Equilibrator temperature
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPEQMN'),
    0, 3, 4, NULL, NULL, NULL
  );
  
-- xCO2  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'MapCO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE name = 'xCO₂ water (dry, no standards)'),
    1, 3, 4, 'xCO₂ in Atmosphere', 'xCO₂ in Atmosphere', 'XCO2DRAT'
  );
