-- Add the zero count and xCO2 Wet as sensor types
INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'ProOceanus Zero Count', 'Sensor Internal', NULL, NULL, NULL, 0, 0, 1255,
    'PROCO2ZERO', 'Zero Count', NULL
  );
  
INSERT INTO sensor_types (
    name, vargroup, parent, depends_on, depends_question, internal_calibration,
    diagnostic, display_order, column_code, column_heading, units
  ) VALUES (
    'xCO₂ (wet, no standards)', 'CO₂', NULL, NULL, NULL, 0, 0, 999,
    'XCO2WTEQ', 'xCO₂', 'μmol mol-1'
  );
  
-- Alter existing Contros sensor types for reuse
UPDATE sensor_types SET
  name = 'Gas Stream Pressure',
  column_code = 'GASPRES'
  WHERE column_code = 'CONTROSGASPRES';

UPDATE sensor_types SET
  name = 'Membrane Pressure',
  column_code = 'MEMBRANEPRES'
  WHERE column_code = 'CONTROSMEMBRANEPRES';

UPDATE sensor_types SET
  name = 'Gas Stream Temperature',
  column_code = 'GASTEMP'
  WHERE column_code = 'CONTROSGASTEMP';

UPDATE sensor_types SET
  name = 'Membrane Temperature',
  column_code = 'MEMBRANETEMP'
  WHERE column_code = 'CONTROSMEMBRANETEMP';

-- Add the Pro Oceanus variables
INSERT INTO variables (name, attributes, properties)
  VALUES ('Pro Oceanus CO₂ Water', NULL, '{"runType": "w m"}');
  
INSERT INTO variables (name, attributes, properties)
  VALUES ('Pro Oceanus CO₂ Atmosphere', NULL, '{"runType": "a m"}');

-- Water sensor types

-- SST
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'TEMPPR01'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Pressure at membrane
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'MEMBRANEPRES'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Zero count
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'PROCO2ZERO'),
    0, 3, 4, NULL, NULL, NULL
  );

-- xCO2  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Water'),
    (SELECT id FROM sensor_types WHERE column_code = 'XCO2WTEQ'),
    1, 3, 4, 'xCO₂ in Water', 'xCO₂ in Water', 'XCO2WTEQ'
  );


-- Atmosphere sensor types

-- Zero count
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'PROCO2ZERO'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Membrane pressure
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'MEMBRANEPRES'),
    0, 3, 4, NULL, NULL, NULL
  );

-- Air temperature
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'CTMPZZ01'),
    0, 3, 4, NULL, NULL, NULL
  );
  
-- xCO2  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, questionable_cascade, bad_cascade,
    export_column_short, export_column_long, export_column_code
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Pro Oceanus CO₂ Atmosphere'),
    (SELECT id FROM sensor_types WHERE column_code = 'XCO2WTEQ'),
    1, 3, 4, 'xCO₂ in Atmosphere', 'xCO₂ in Atmosphere', 'XCO2DCMA'
  );
