-- New sensor type
INSERT INTO sensor_types (name) VALUES ('Unobtanium');

-- New variable
INSERT INTO variables (name) VALUES ('Unobtanium');

-- Variable sensors - core
INSERT INTO variable_sensors VALUES (
  (SELECT id FROM variables WHERE name = 'Unobtanium'),
  (SELECT id FROM sensor_types WHERE name = 'Unobtanium'),
  1, 3, 4);

