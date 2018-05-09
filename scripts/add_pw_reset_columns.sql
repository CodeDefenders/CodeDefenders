ALTER TABLE users ADD pw_reset_timestamp TIMESTAMP DEFAULT NULL  NULL;
ALTER TABLE users ADD pw_reset_secret VARCHAR(254) DEFAULT NULL  NULL;
CREATE UNIQUE INDEX users_pw_reset_secret_uindex ON users (pw_reset_secret);