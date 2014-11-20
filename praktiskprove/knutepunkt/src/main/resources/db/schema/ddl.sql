CREATE TABLE EVENT_LOG (
                UUID VARCHAR(36), 
                SENDER VARCHAR(9), 
                RECEIVER VARCHAR(9), 
                EVENT_TIMESTAMP BIGINT,
                STATE VARCHAR(50), 
                ERROR_MESSAGE  LONG VARCHAR,
                MESSAGE LONG VARCHAR,
                PRIMARY KEY(UUID));
