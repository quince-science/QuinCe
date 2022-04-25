-- We don't want equilibrator temperatures for Pro Oceanus/SAMI sensors
DELETE FROM variable_sensors WHERE variable_id = 7 AND sensor_type = 3;
DELETE FROM variable_sensors WHERE variable_id = 8 AND sensor_type = 3;
