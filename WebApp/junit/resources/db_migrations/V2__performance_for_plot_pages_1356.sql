CREATE INDEX USERQCFLAG ON sensor_values (user_qc_flag);
CREATE INDEX SV_DATASETID_DATE ON sensor_values (dataset_id, date);
CREATE INDEX FILECOLUMN ON sensor_values (file_column);
CREATE INDEX MEAS_DATASETID_DATE ON measurements (dataset_id, date);


-- ROLLBACK

-- DROP INDEX USERQCFLAG ON sensor_values;
-- DROP INDEX SV_DATASETID_DATE ON sensor_values;
-- DROP INDEX FILECOLUMN ON sensor_values;
-- DROP INDEX MEAS_DATASETID_DATE ON measurements;
