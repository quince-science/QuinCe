# Add id field to file_column table
ALTER TABLE file_column DROP FOREIGN KEY FILECOLUMN_FILEDEFINITION;
ALTER TABLE file_column DROP PRIMARY KEY;
ALTER TABLE `file_column` ADD `id` INT(11) NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (`id`);
ALTER TABLE `file_column` ADD CONSTRAINT `FILECOLUMN_FILEDEFINITION` FOREIGN KEY (`file_definition_id`) REFERENCES `file_definition`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `file_column` ADD UNIQUE `FILEDEFINITIONID_FILECOLUMN` (`file_definition_id`, `file_column`);


# Table for diagnostic data - one record per sensor per measurement
CREATE TABLE `diagnostic_data` (
  `measurement_id` INT NOT NULL,
  `file_column_id` INT NOT NULL,
  `value` DOUBLE NOT NULL,
  PRIMARY KEY (`file_column_id`, `measurement_id`),
  INDEX `DIAGNOSTICDATA_FILECOLUMN_idx` (`file_column_id` ASC),
  CONSTRAINT `DIAGNOSTICDATA_DATASETDATA`
    FOREIGN KEY (`measurement_id`)
    REFERENCES `dataset_data` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `DIAGNOSTICDATA_FILECOLUMN`
    FOREIGN KEY (`file_column_id`)
    REFERENCES `file_column` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT);

# Remove the NOT NULL constraint on the value_column field - diagnostic
# values don't need one
ALTER TABLE `file_column` CHANGE `value_column` `value_column` SMALLINT(3) NULL;
