-- Benguela Stream instrument definition

-- Instrument
INSERT INTO instrument VALUES (1,1,'Benguela Stream',0,0,-1,0,'BSBS',0,'2019-01-28 13:31:21','2019-01-28 14:31:21');

-- Instrument uses the basic marine pCO2 variable
INSERT INTO instrument_variables (instrument_id, variable_id)
  VALUES (1, (SELECT id FROM variables WHERE name = 'Underway Marine pCO₂'));
  
-- File definition
INSERT INTO file_definition VALUES
  (1,1,'Data File',' ',0,0,NULL,2,65,2,7,8,1,5,6,-1,NULL,3,
   '#Mon Jan 28 14:31:21 CET 2019\nformatString=dd/MM/yy\n',
   -1,NULL,-1,-1,-1,-1,-1,4,
   '#Mon Jan 28 14:31:21 CET 2019\nformatString=HH\\:mm\\:ss\n',
   -1,-1,-1,'2019-01-28 13:31:21','2019-01-28 14:31:21');
   
-- File Columns

-- Intake temperature   
INSERT INTO file_column VALUES (1,1,14,1,1,'SWTemp',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');

-- Salinity
INSERT INTO file_column VALUES (2,1,63,1,2,'SBSal',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');

-- Equilibrator Temperature
INSERT INTO file_column VALUES (3,1,19,1,3,'PT100_1',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');

-- Equilibrator Pressure (absolute)
INSERT INTO file_column VALUES (4,1,36,1,5,'Pres2',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');

-- CO2
INSERT INTO file_column VALUES (5,1,40,1,9,'LCO2D',0,'','2019-01-28 13:31:21','2019-01-28 14:31:21');

-- Run Type
INSERT INTO file_column VALUES (6,1,2,1,-1,'Run Type',0,NULL,'2019-01-28 13:31:21','2019-01-28 14:31:21');

