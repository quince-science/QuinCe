CREATE TABLE sensor_values (
  dataset_id INT NOT NULL,
  file_column INT NOT NULL,
  date BIGINT(20) NOT NULL,
  value VARCHAR(100) NULL,
  CONSTRAINT SENSORVALUE_DATASET
    FOREIGN KEY (dataset_id)
    REFERENCES dataset (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

-- Remove value_column from file_column table - no longer used
ALTER TABLE file_column DROP COLUMN value_column;
