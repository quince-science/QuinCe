-- Rename the existing messages_json field
ALTER TABLE dataset CHANGE messages_json error_messages TEXT;

-- Add new message fields
ALTER TABLE dataset 
  ADD COLUMN processing_messages TEXT NULL DEFAULT NULL AFTER error_messages,
  ADD COLUMN user_messages TEXT NULL DEFAULT NULL AFTER processing_messages;
