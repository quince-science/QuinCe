-- Allow file columns to be assigned to more than one SensorType
ALTER TABLE file_column 
  DROP FOREIGN KEY FILECOLUMN_FILEDEFINITION;

ALTER TABLE file_column DROP INDEX FILEDEFINITIONID_FILECOLUMN;
ALTER TABLE file_column
  ADD UNIQUE INDEX FILEDEF_COL_SENSOR (file_definition_id ASC, file_column ASC, sensor_type ASC);
  
ALTER TABLE file_column 
  ADD CONSTRAINT FILECOL_FILEDEF
    FOREIGN KEY (file_definition_id)
    REFERENCES file_definition (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;