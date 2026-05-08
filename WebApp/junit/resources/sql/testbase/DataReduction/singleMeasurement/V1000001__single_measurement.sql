-- Create sensor values and a measurement all for a single timestamp

-- Coordinates
INSERT INTO coordinates (id, dataset_id, date) VALUES (1,1,1704067200000);

-- Run Type
INSERT INTO sensor_values VALUES (1,1,1,'var_1',NULL,-2,NULL);

-- SST value
INSERT INTO sensor_values VALUES (2,1,2,'20',NULL,-2,NULL);

-- Salinity value
INSERT INTO sensor_values VALUES (3,1,3,'35',NULL,-2,NULL);

-- CO2 value
INSERT INTO sensor_values VALUES (4,1,4,'375',NULL,-2,NULL);

-- Diagnostic water
INSERT INTO sensor_values VALUES (5,1,5,'2',NULL,-2,NULL);

-- Diagnostic gas
INSERT INTO sensor_values VALUES (6,1,6,'10',NULL,-2,NULL);
