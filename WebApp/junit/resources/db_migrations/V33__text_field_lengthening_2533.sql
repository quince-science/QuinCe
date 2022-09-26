-- Lengthen text fields for greater flexibility

-- Dataset messages
ALTER TABLE dataset MODIFY error_messages MEDIUMTEXT;
ALTER TABLE dataset MODIFY processing_messages MEDIUMTEXT;
ALTER TABLE dataset MODIFY user_messages MEDIUMTEXT;

-- File definition header end string
ALTER TABLE file_definition MODIFY header_end_string TEXT;