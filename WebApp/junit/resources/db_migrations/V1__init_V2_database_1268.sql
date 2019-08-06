-- Initialisation of the QuinCe v2 database
-- Based on a MySQL export
--
-- Assumes that the target database is empty
--
-- Table structures are define first, then populated
-- at the end

--
-- Table structure for table `user`
--

CREATE TABLE user (
  id int(11) NOT NULL AUTO_INCREMENT,
  email varchar(45) NOT NULL,
  salt varbinary(20) NOT NULL,
  password varbinary(45) NOT NULL,
  firstname varchar(30) DEFAULT NULL,
  surname varchar(45) DEFAULT NULL,
  email_code varchar(50) DEFAULT NULL,
  email_code_time timestamp NULL DEFAULT NULL,
  password_code varchar(50) DEFAULT NULL,
  password_code_time timestamp NULL DEFAULT NULL,
  permissions int(11) NOT NULL DEFAULT '0',
  preferences mediumtext,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY email_UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `sensor_types`
--

CREATE TABLE sensor_types (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  vargroup varchar(100) NOT NULL,
  parent int(11) DEFAULT NULL,
  depends_on int(11) DEFAULT NULL,
  depends_question text,
  internal_calibration tinyint(4) NOT NULL DEFAULT '0',
  diagnostic tinyint(4) NOT NULL DEFAULT '0',
  display_order int(11) NOT NULL,
  column_code varchar(50) DEFAULT NULL,
  column_heading varchar(100) DEFAULT NULL,
  units varchar(20) DEFAULT NULL,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY SENSORTYPENAME (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `variables`
--

CREATE TABLE variables (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  attributes mediumtext,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY VARNAME_UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `variable_sensors`
--

CREATE TABLE variable_sensors (
  variable_id int(11) NOT NULL,
  sensor_type int(11) NOT NULL,
  core tinyint(1) NOT NULL DEFAULT '0',
  questionable_cascade tinyint(1) NOT NULL,
  bad_cascade tinyint(1) NOT NULL,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (variable_id,sensor_type),
  CONSTRAINT VARSENSOR_SENSOR FOREIGN KEY (sensor_type) REFERENCES sensor_types (id),
  CONSTRAINT VARSENSOR_VARIABLE FOREIGN KEY (variable_id) REFERENCES variables (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX VARSENSOR_SENSOR_idx ON variable_sensors(sensor_type);

--
-- Table structure for table `instrument`
--

CREATE TABLE instrument (
  id int(11) NOT NULL AUTO_INCREMENT,
  owner int(11) NOT NULL,
  name varchar(100) NOT NULL,
  pre_flushing_time int(11) DEFAULT '0',
  post_flushing_time int(11) DEFAULT '0',
  depth int(11) NOT NULL DEFAULT '0',
  platform_code varchar(6) DEFAULT NULL,
  nrt tinyint(1) NOT NULL DEFAULT '0',
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT INSTRUMENT_OWNER FOREIGN KEY (owner) REFERENCES user (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX OWNER_idx ON instrument(owner);

--
-- Table structure for table `instrument_variables`
--

CREATE TABLE instrument_variables (
  instrument_id int(11) NOT NULL,
  variable_id int(11) NOT NULL,
  attributes mediumtext,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  CONSTRAINT INSTRVAR_INSTRUMENT FOREIGN KEY (instrument_id) REFERENCES instrument (id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT INSTRVAR_VARIABLE FOREIGN KEY (variable_id) REFERENCES variables (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX INSTRVAR_INSTRUMENT_idx ON instrument_variables(instrument_id);
CREATE INDEX INSTRVAR_VARIABLE_idx ON instrument_variables(variable_id);

--
-- Table structure for table `file_definition`
--

CREATE TABLE file_definition (
  id int(11) NOT NULL AUTO_INCREMENT,
  instrument_id int(11) NOT NULL,
  description varchar(100) NOT NULL,
  column_separator varchar(1) NOT NULL,
  header_type tinyint(1) NOT NULL,
  header_lines smallint(3) DEFAULT NULL,
  header_end_string varchar(100) DEFAULT NULL,
  column_header_rows tinyint(2) NOT NULL,
  column_count smallint(3) NOT NULL,
  lon_format tinyint(1) NOT NULL DEFAULT '-1',
  lon_value_col smallint(3) NOT NULL DEFAULT '-1',
  lon_hemisphere_col smallint(3) NOT NULL DEFAULT '-1',
  lat_format tinyint(1) NOT NULL DEFAULT '-1',
  lat_value_col smallint(3) NOT NULL DEFAULT '-1',
  lat_hemisphere_col smallint(3) NOT NULL DEFAULT '-1',
  date_time_col smallint(3) NOT NULL DEFAULT '-1',
  date_time_props text,
  date_col smallint(3) NOT NULL DEFAULT '-1',
  date_props text,
  hours_from_start_col smallint(3) NOT NULL DEFAULT '-1',
  hours_from_start_props text,
  jday_time_col smallint(3) NOT NULL DEFAULT '-1',
  jday_col smallint(3) NOT NULL DEFAULT '-1',
  year_col smallint(3) NOT NULL DEFAULT '-1',
  month_col smallint(3) NOT NULL DEFAULT '-1',
  day_col smallint(3) NOT NULL DEFAULT '-1',
  time_col smallint(3) NOT NULL DEFAULT '-1',
  time_props text,
  hour_col smallint(3) NOT NULL DEFAULT '-1',
  minute_col smallint(3) NOT NULL DEFAULT '-1',
  second_col smallint(3) NOT NULL DEFAULT '-1',
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FILEDEFINITION_INSTRUMENT FOREIGN KEY (instrument_id) REFERENCES instrument (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX FILEDEFINITION_INSTRUMENT_idx ON file_definition(instrument_id);

--
-- Table structure for table `file_column`
--

CREATE TABLE file_column (
  id int(11) NOT NULL AUTO_INCREMENT,
  file_definition_id int(11) NOT NULL,
  file_column smallint(3) NOT NULL,
  primary_sensor tinyint(1) NOT NULL,
  sensor_type int(11) DEFAULT NULL,
  sensor_name varchar(100) NOT NULL,
  depends_question_answer tinyint(1) NOT NULL DEFAULT '0',
  missing_value varchar(50) DEFAULT NULL,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY FILEDEFINITIONID_FILECOLUMN (file_definition_id,file_column),
  CONSTRAINT FILECOLUMN_FILEDEFINITION FOREIGN KEY (file_definition_id) REFERENCES file_definition (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `run_type`
--

CREATE TABLE run_type (
  file_definition_id int(11) NOT NULL,
  run_name varchar(50) NOT NULL,
  category_code int(11) NOT NULL,
  alias_to varchar(50) DEFAULT NULL,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (file_definition_id,run_name),
  CONSTRAINT RUNTYPE_FILEDEFINITION FOREIGN KEY (file_definition_id) REFERENCES file_definition (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `calibration`
--

CREATE TABLE calibration (
  instrument_id int(11) NOT NULL,
  type varchar(20) NOT NULL,
  target varchar(45) NOT NULL,
  deployment_date bigint(20) NOT NULL,
  coefficients text NOT NULL,
  class varchar(45) NOT NULL,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (instrument_id,type,target,deployment_date),
  CONSTRAINT CALIBRATION_INSTRUMENT FOREIGN KEY (instrument_id) REFERENCES instrument (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `data_file`
--

CREATE TABLE data_file (
  id int(11) NOT NULL AUTO_INCREMENT,
  file_definition_id int(11) NOT NULL,
  filename varchar(200) NOT NULL,
  start_date bigint(20) NOT NULL,
  end_date bigint(20) NOT NULL,
  record_count int(11) NOT NULL,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT DATAFILE_FILEDEFINITION FOREIGN KEY (file_definition_id) REFERENCES file_definition (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX DATAFILE_FILEDEFINITION_idx ON data_file(file_definition_id);

--
-- Table structure for table `dataset`
--

CREATE TABLE dataset (
  id int(11) NOT NULL AUTO_INCREMENT,
  instrument_id int(11) NOT NULL,
  name varchar(100) NOT NULL,
  start bigint(20) NOT NULL,
  end bigint(20) NOT NULL,
  min_longitude double DEFAULT NULL,
  max_longitude double DEFAULT NULL,
  min_latitude double DEFAULT NULL,
  max_latitude double DEFAULT NULL,
  status tinyint(1) NOT NULL,
  nrt tinyint(1) NOT NULL DEFAULT '0',
  status_date bigint(20) NOT NULL,
  properties text,
  messages_json text,
  last_touched bigint(20) DEFAULT NULL,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT DATASET_INSTRUMENT FOREIGN KEY (instrument_id) REFERENCES instrument (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX DATASET_INSTRUMENT_idx ON dataset(instrument_id);

--
-- Table structure for table `sensor_values`
--

CREATE TABLE sensor_values (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  dataset_id int(11) NOT NULL,
  file_column int(11) NOT NULL,
  date bigint(20) NOT NULL,
  value varchar(100) DEFAULT NULL,
  auto_qc text,
  user_qc_flag smallint(2) DEFAULT '-1000',
  user_qc_message varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT SENSORVALUE_DATASET FOREIGN KEY (dataset_id) REFERENCES dataset (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX SENSORVALUE_DATASET_idx ON sensor_values(dataset_id);

--
-- Table structure for table `measurements`
--

CREATE TABLE measurements (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  dataset_id int(11) NOT NULL,
  variable_id int(11) NOT NULL,
  date bigint(20) NOT NULL,
  longitude double NOT NULL,
  latitude double NOT NULL,
  run_type varchar(45) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT measurement_dataset FOREIGN KEY (dataset_id) REFERENCES dataset (id) ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX MEASUREMENT_DATASET_idx ON measurements(dataset_id);

--
-- Table structure for table `measurement_values`
--

CREATE TABLE measurement_values (
  measurement_id bigint(20) NOT NULL,
  variable_id int(11) NOT NULL,
  sensor_value_id bigint(20) NOT NULL,
  PRIMARY KEY (measurement_id,variable_id,sensor_value_id),
  CONSTRAINT MEASVAL_MEASUREMENT FOREIGN KEY (measurement_id) REFERENCES measurements (id),
  CONSTRAINT MEASVAL_SENSORVALUE FOREIGN KEY (sensor_value_id) REFERENCES sensor_values (id),
  CONSTRAINT MEASVAL_VARIABLE FOREIGN KEY (variable_id) REFERENCES variables (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX MEASVAL_MEASUREMENT_idx ON measurement_values(measurement_id);
CREATE INDEX MEASVAL_VARIABLE_idx ON measurement_values(variable_id);
CREATE INDEX MEASVAL_SENSORVALUE_idx ON measurement_values(sensor_value_id);

--
-- Table structure for table `data_reduction`
--

CREATE TABLE data_reduction (
  measurement_id bigint(20) NOT NULL,
  variable_id int(11) NOT NULL,
  calculation_values mediumtext NOT NULL,
  qc_flag smallint(2) NOT NULL,
  qc_message text,
  PRIMARY KEY (measurement_id,variable_id),
  CONSTRAINT DATAREDUCTION_MEASUREMENT FOREIGN KEY (measurement_id) REFERENCES measurements (id) ON UPDATE NO ACTION,
  CONSTRAINT DATAREDUCTION_VARIABLE FOREIGN KEY (variable_id) REFERENCES variables (id) ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX DATAREDUCTION_VARIABLE_idx ON data_reduction(variable_id);

--
-- Table structure for table `job`
--

CREATE TABLE job (
  id int(11) NOT NULL AUTO_INCREMENT,
  owner int(11) DEFAULT NULL,
  class text NOT NULL,
  parameters longtext,
  status enum('WAITING','RUNNING','FINISHED','ERROR','KILLED') NOT NULL DEFAULT 'WAITING',
  started timestamp NULL DEFAULT NULL,
  ended timestamp NULL DEFAULT NULL,
  thread_name varchar(50) DEFAULT NULL,
  progress float DEFAULT NULL,
  stack_trace longtext,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT JOB_OWNER FOREIGN KEY (owner) REFERENCES user (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX JOB_OWNER_idx ON job(owner);

--------------------------------------------------------------
--
-- Initialise data

-- Sensor Types
INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (1, 'Intake Temperature', 'Temperature', NULL, NULL, NULL, 0, 0, 1, 'TEMPPR01', 'Water Temperature', '°C');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (2, 'Salinity', 'Salinity', NULL, NULL, NULL, 0, 0, 2, 'PSALPR01', 'Practical Salinity', 'psu');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (3, 'Equilibrator Temperature', 'Temperature', NULL, NULL, NULL, 0, 0, 3, 'TEMPEQMN', 'Temperature of Equilibration', '°C');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (4, 'Equilibrator Pressure', 'Pressure', NULL, NULL, NULL, 0, 0, 4, 'PRESSEQ', 'Pressure in Equilibrator', 'hPa');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (5, 'Equilibrator Pressure (absolute)', 'Pressure', 4, NULL, NULL, 0, 0, 4, 'PRESEQABS', 'Absolute Pressure in Equilibrator', 'hPa');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (6, 'Equilibrator Pressure (differential)', 'Pressure', 4, 7, NULL, 0, 0, 4, 'PRESEQREL', 'Relative Pressure in Equilibrator', 'hPa');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (7, 'Ambient Pressure', 'Pressure', NULL, NULL, NULL, 0, 0, 5, 'PRESAMB', 'Instrument Ambient Pressure', 'hPa');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (8, 'xH₂O in gas', 'Moisture', NULL, NULL, NULL, 1, 0, 6, 'WMXRZZ01', 'H₂O Mole Fraction', 'μmol mol-1');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (9, 'CO₂ in gas', 'CO₂', NULL, 8, 'Do values from CO₂ require moisture adjustment?', 1, 0, 7, 'XCO2EQWT', 'CO₂ Mole Fraction', 'μmol mol-1');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (10, 'Atmospheric Pressure', 'Pressure', NULL, NULL, NULL, 0, 0, 8, 'CAPHZZ01', 'Atmospheric Pressure', 'hPa');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (11, 'Diagnostic Temperature', 'Temperature', NULL, NULL, NULL, 0, 1, 9, NULL, NULL, NULL);

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (12, 'Diagnostic Pressure', 'Pressure', NULL, NULL, NULL, 0, 1, 10, NULL, NULL, NULL);

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (13, 'Diagnostic Gas Flow', 'Other', NULL, NULL, NULL, 0, 1, 11, NULL, NULL, NULL);

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (14, 'Diagnostic Water Flow', 'Other', NULL, NULL, NULL, 0, 1, 12, NULL, NULL, NULL);

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (15, 'Diagnostic Voltage', 'Other', NULL, NULL, NULL, 0, 1, 13, NULL, NULL, NULL);

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (16, 'Diagnostic Misc', 'Other', NULL, NULL, NULL, 0, 1, 14, NULL, NULL, NULL);

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (17, 'Speed', 'Other', NULL, NULL, NULL, 0, 0, 15, 'APSAZZ01', 'Speed', 'm s-1');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (18, 'Heading', 'Other', NULL, NULL, NULL, 0, 0, 16, 'HDNGGP01', 'Heading', 'deg');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (19, 'Wind Speed (absolute)', 'Other', NULL, NULL, NULL, 0, 0, 17, 'EWSBZZ01', 'Wind Speed (absolute)', 'm s-1');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (20, 'Wind Speed (relative)', 'Other', NULL, NULL, NULL, 0, 0, 18, 'ERWDZZ01', 'Wind Speed (relative)', 'deg');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (21, 'Wind Direction (absolute)', 'Other', NULL, NULL, NULL, 0, 0, 19, 'EWDAZZ01', 'Wind Direction (absolute)', 'deg');

INSERT INTO sensor_types
  (id, name, vargroup, parent, depends_on, depends_question, internal_calibration, diagnostic, display_order, column_code, column_heading, units)
  VALUES (22, 'Wind Direction (relative)', 'Other', NULL, NULL, NULL, 0, 0, 20, 'ERWDZZ01', 'Wind Direction (relative)', 'deg');

-- Variables
INSERT INTO variables (id, name, attributes)
  VALUES (1, 'Underway Marine pCO₂', NULL);

INSERT INTO variables (id, name, attributes)
  VALUES (2, 'Underway Atmospheric pCO₂', '{"atm_pres_sensor_height": "Atmospheric Pressure Sensor Height"}');


-- Variable Sensors
INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (1, 1, 0, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (1, 2, 0, 2, 3);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (1, 3, 0, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (1, 4, 0, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (1, 9, 1, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (2, 2, 0, 2, 2);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (2, 3, 0, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (2, 9, 1, 3, 4);

INSERT INTO variable_sensors
  (variable_id, sensor_type, core, questionable_cascade, bad_cascade)
  VALUES (2, 10, 0, 3, 4);
