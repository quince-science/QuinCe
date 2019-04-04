-- Add ship speed and direction sensors
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ship Speed', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Ship Course', 'Other', NULL, NULL, NULL, 0);

-- Add wind speed and direction sensors. Speeds can be
INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Speed', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Direction (absolute)', 'Other', NULL, NULL, NULL, 0);

INSERT INTO sensor_types (name, vargroup, parent, depends_on, depends_question, internal_calibration)
  VALUES ('Wind Direction (relative)', 'Other', NULL, NULL, NULL, 0);

CREATE TABLE sensor_values (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  dataset_id INT NOT NULL,
  file_column INT NOT NULL,
  date BIGINT(20) NOT NULL,
  value VARCHAR(100) NULL,
  auto_qc TEXT,
  user_qc_flag SMALLINT(2) DEFAULT -1000,
  user_qc_message VARCHAR(255),
  CONSTRAINT SENSORVALUE_DATASET
    FOREIGN KEY (dataset_id)
    REFERENCES dataset (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

-- Remove value_column from file_column table - no longer used
ALTER TABLE file_column DROP COLUMN value_column;
