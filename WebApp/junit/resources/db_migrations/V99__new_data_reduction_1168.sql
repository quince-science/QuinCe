-- Changes for the new data reduction scheme
CREATE TABLE measurements (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  dataset_id INT NOT NULL,
  variable_id INT NOT NULL,
  date BIGINT(20) NOT NULL,
  longitude DOUBLE NOT NULL,
  latitude DOUBLE NOT NULL,
  run_type VARCHAR(45) NULL,
  PRIMARY KEY (id),
  CONSTRAINT measurement_dataset
    FOREIGN KEY (dataset_id)
    REFERENCES dataset (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

CREATE INDEX measurement_dataset_idx ON measurements(dataset_id ASC);

CREATE TABLE measurement_values (
  measurement_id BIGINT(20) NOT NULL,
  variable_id INT(11) NOT NULL,
  sensor_value_id BIGINT(20) NOT NULL,
  PRIMARY KEY (measurement_id, variable_id, sensor_value_id),
  CONSTRAINT MEASVAL_MEASUREMENT
    FOREIGN KEY (measurement_id)
    REFERENCES measurements (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT MEASVAL_VARIABLE
    FOREIGN KEY (variable_id)
    REFERENCES variables (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT MEASVAL_SENSORVALUE
    FOREIGN KEY (sensor_value_id)
    REFERENCES sensor_values (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

CREATE INDEX measval_measurements_idx ON measurement_values(measurement_id ASC);

CREATE TABLE data_reduction (
  measurement_id BIGINT(20) NOT NULL,
  variable_id INT(11) NOT NULL,
  calculation_values MEDIUMTEXT NOT NULL,
  qc_flag SMALLINT(2) NOT NULL,
  qc_message TEXT NULL,
  PRIMARY KEY (measurement_id, variable_id),
  CONSTRAINT DATAREDUCTION_MEASUREMENT
    FOREIGN KEY (measurement_id)
    REFERENCES measurements (id)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION,
  CONSTRAINT DATAREDUCTION_VARIABLE
    FOREIGN KEY (variable_id)
    REFERENCES variables (id)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION);
    
CREATE INDEX datareduction_variable_idx ON data_reduction(variable_id ASC); 