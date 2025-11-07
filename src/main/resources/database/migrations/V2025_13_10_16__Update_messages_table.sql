ALTER TABLE messages
DROP FOREIGN KEY fk_message_session;

ALTER TABLE messages
CHANGE COLUMN session_id conversation_id INT UNSIGNED NOT NULL;

ALTER TABLE messages
ADD CONSTRAINT fk_messages_conversation
    FOREIGN KEY (conversation_id) REFERENCES conversations(id);
