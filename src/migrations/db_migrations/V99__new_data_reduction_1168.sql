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
  INDEX measurement_dataset_idx (dataset_id ASC),
  CONSTRAINT measurement_dataset
    FOREIGN KEY (dataset_id)
    REFERENCES dataset (id)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION);

CREATE TABLE measurement_values (
  measurement_id BIGINT(20) NOT NULL,
  sensor_value_id BIGINT(20) NOT NULL,
  PRIMARY KEY (measurement_id, sensor_value_id),
  INDEX MEASVAL_MEASUREMENT_idx (measurement_id ASC),
  CONSTRAINT MEASVAL_MEASUREMENT
    FOREIGN KEY (measurement_id)
    REFERENCES measurements (id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT MEASVAL_SENSORVALUE
    FOREIGN KEY (sensor_value_id)
    REFERENCES sensor_values (id)
    ON DELETE RESTRICT
    ON UPDATE NO ACTION);

-- Drop the old equilibrator_pco2 table - we don't
-- want it any more (old datasets will be obsolete at this point)
DROP TABLE IF EXISTS equilibrator_pco2;

-- Create the new equilibrator_pco2 table
CREATE TABLE equilibrator_pco2 (
  measurement_id BIGINT(20) NOT NULL,
  delta_temperature double DEFAULT NULL,
  true_moisture double DEFAULT NULL,
  ph2o double DEFAULT NULL,
  dried_co2 double DEFAULT NULL,
  calibrated_co2 double DEFAULT NULL,
  pco2_te_wet double DEFAULT NULL,
  pco2_sst double DEFAULT NULL,
  fco2 double DEFAULT NULL,
  qc_flag smallint(2) DEFAULT '-1000',
  qc_message text,
  created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified datetime DEFAULT NULL,
  PRIMARY KEY (measurement_id),
  CONSTRAINT EQPCO2_MEASUREMENT FOREIGN KEY (measurement_id)
    REFERENCES measurements (id) ON DELETE RESTRICT ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;