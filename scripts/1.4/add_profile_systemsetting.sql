/* Insert "ALLOW_USER_PROFILE" row to system setting table. */
INSERT INTO settings (name, type, STRING_VALUE, INT_VALUE, BOOL_VALUE) VALUES
  ('ALLOW_USER_PROFILE', 'BOOL_VALUE', NULL, NULL, TRUE);
