DROP TABLE IF EXISTS `settings`;
CREATE TABLE settings
(
  name         VARCHAR(50) PRIMARY KEY NOT NULL,
  type         ENUM ('STRING_VALUE', 'INT_VALUE', 'BOOL_VALUE'),
  STRING_VALUE TEXT,
  INT_VALUE    INTEGER,
  BOOL_VALUE   BOOL
);

INSERT INTO settings (name, type, STRING_VALUE, INT_VALUE, BOOL_VALUE) VALUES
  ('SHOW_PLAYER_FEEDBACK', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('REGISTRATION', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('CLASS_UPLOAD', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('GAME_CREATION', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('REQUIRE_MAIL_VALIDATION', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('SITE_NOTICE', 'STRING_VALUE', 'please add a site notice', NULL, NULL),
  ('PASSWORD_RESET_SECRET_LIFESPAN', 'INT_VALUE', NULL, 12, NULL),
  ('MIN_PASSWORD_LENGTH', 'INT_VALUE', NULL, 8, NULL),
  ('CONNECTION_POOL_CONNECTIONS', 'INT_VALUE', NULL, 20, NULL),
  ('CONNECTION_WAITING_TIME', 'INT_VALUE', NULL, 5000, NULL),
  ('EMAIL_SMTP_HOST', 'STRING_VALUE', '', NULL, NULL),
  ('EMAIL_SMTP_PORT', 'INT_VALUE', '', NULL, NULL),
  ('EMAIL_ADDRESS', 'STRING_VALUE', '', NULL, NULL),
  ('EMAILS_ENABLED', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('DEBUG_MODE', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('EMAIL_PASSWORD', 'STRING_VALUE', '', NULL, NULL);
