CREATE TABLE queue_metadata (
  unique_id VARCHAR(32) PRIMARY KEY,
  numberAttempt INTEGER,
  rule VARCHAR(16),
  lastAttemptTime TIMESTAMP,
  requestLocation VARCHAR(256),
  status VARCHAR(16),
  checksum VARCHAR(256)
);