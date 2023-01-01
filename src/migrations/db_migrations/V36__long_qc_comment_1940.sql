-- Change user QC message to a text field to accommodate lookup IDs
ALTER TABLE sensor_values CHANGE COLUMN user_qc_message user_qc_message TEXT NULL DEFAULT NULL;
