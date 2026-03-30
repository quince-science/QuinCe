-- This is a temporary setup for development. It IS NOT COMPLETE and WILL CHANGE

--
INSERT INTO sensor_types (
    name, vargroup, display_order, column_code
  ) VALUES (
    'DOXY', 'DOXY', 2400, 'DOXY'
  );

INSERT INTO variables (name, allowed_basis, attributes, properties)
  VALUES ('Aanderaa 4330 Oxygen', 3, NULL, '{"coefficients": ["Coef1", "Coef2"]}');

INSERT INTO variable_sensors (
    variable_id, sensor_type, core, cascades
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Aanderaa 4330 Oxygen'),
    (SELECT id FROM sensor_types WHERE name = 'DOXY'),
    1, '{"Time":[[3,3],[4,4]]}'
  );

  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, cascades
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Aanderaa 4330 Oxygen'),
    (SELECT id FROM sensor_types WHERE name = 'Water Temperature'),
    0, '{"Time":[[3,3],[4,4]]}'
  );

  
INSERT INTO variable_sensors (
    variable_id, sensor_type, core, cascades
  )
  VALUES (
    (SELECT id FROM variables WHERE name = 'Aanderaa 4330 Oxygen'),
    (SELECT id FROM sensor_types WHERE name = 'Salinity'),
    0, '{"Time":[[3,3],[4,4]]}'
  );
