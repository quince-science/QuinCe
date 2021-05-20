-- Generic xCO2 dry sensor type

INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'xCO₂ (dry, no standards)', 'CO₂', NULL, NULL, NULL, 0, 0, 998,
    'XCO2WBDY', 'CO₂ Mole Fraction', NULL
  );
  

-- MapCO2/ASVCO2 variables

-- Add the MapCo2 variables
INSERT INTO variables (name, attributes, properties)
  VALUES ('ASVCO₂ Water', NULL, '{"runType": "EP"}');
  
INSERT INTO variables (name, attributes, properties)
  VALUES ('ASVCO₂ Atmosphere', NULL, '{"runType": "AP"}');

-- Water sensor types

-- SST
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPPR01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Salinity
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PSALPR01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Equilibrator temperature
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPEQMN'),
    0, 3, 4, NULL, NULL, NULL
  );

  
-- Equilibrator Pressure
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PRESSEQ'),
    0, 3, 4, NULL, NULL, NULL
  );

-- xCO2  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Water'),
    (SELECT id FROM sensor_types WHERE name = 'xCO₂ (dry, no standards)'),
    1, 3, 4, 'xCO₂ in Water', 'xCO₂ in Water', 'XCO2WBDY'
  );


-- Atmosphere sensor types
-- Atmospheric pressure
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'CAPHZZ01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Equilibrator temperature
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPEQMN'),
    0, 3, 4, NULL, NULL, NULL
  );
  
-- xCO2  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'ASVCO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE name = 'xCO₂ (dry, no standards)'),
    1, 3, 4, 'xCO₂ in Atmosphere', 'xCO₂ in Atmosphere', 'XCO2DRAT'
  );
