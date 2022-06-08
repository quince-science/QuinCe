-- Rename the existing messages_json field
ALTER TABLE dataset CHANGE messages_json error_messages TEXT;

-- Add new message fields
ALTER TABLE dataset 
  ADD COLUMN processing_messages TEXT NULL DEFAULT NULL AFTER messages_json,
  ADD COLUMN user_messages TEXT NULL DEFAULT NULL AFTER processing_messages;
