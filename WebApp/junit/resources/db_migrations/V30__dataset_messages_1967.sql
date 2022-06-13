-- Rename the existing messages_json field
ALTER TABLE dataset CHANGE messages_json error_messages TEXT;

-- Add new message fields
ALTER TABLE dataset 
  ADD COLUMN processing_messages TEXT DEFAULT NULL AFTER error_messages;
ALTER TABLE dataset 
  ADD COLUMN user_messages TEXT DEFAULT NULL AFTER processing_messages;
