-- MariaDB / MySQL
-- Creates a per-client mapping used by the wrapper token generator.

CREATE TABLE IF NOT EXISTS client_profile (
  client_id VARCHAR(64) NOT NULL,
  transaction_userid VARCHAR(64) NOT NULL,
  transaction_merchantid VARCHAR(64) NOT NULL,
  PRIMARY KEY (client_id)
);

-- Example seed (edit values as needed):
-- INSERT INTO client_profile (client_id, transaction_userid, transaction_merchantid)
-- VALUES ('5e06f31d-d298-11f0-96ff-4201c0a81e02', '317161', '446442');
