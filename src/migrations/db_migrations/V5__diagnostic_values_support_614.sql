# Table for diagnostic data - one record per sensor per measurement
CREATE TABLE `diagnostic`.`diagnostic_data` (
  `measurement_id` INT NOT NULL,
  `sensor_name` VARCHAR(100) NOT NULL,
  `value` DOUBLE NULL,
  PRIMARY KEY (`measurement_id`, `sensor_name`),
  INDEX `DIAGNOSTIC_DATASETDATA_idx` (`measurement_id` ASC),
  CONSTRAINT `DIAGNOSTIC_DATASETDATA`
    FOREIGN KEY (`measurement_id`)
    REFERENCES `diagnostic`.`dataset_data` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

# Remove the NOT NULL constraint on the value_column field - diagnostic
# values don't need one
ALTER TABLE `file_column` CHANGE `value_column` `value_column` SMALLINT(3) NULL;
