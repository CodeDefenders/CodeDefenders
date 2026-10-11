-- Keep the existing shared account and games, but disable email recovery.
UPDATE users
SET Email = 'mutle-guest@codedefenders.invalid', pw_reset_secret = NULL, pw_reset_timestamp = NULL
WHERE User_ID = 8;

CREATE INDEX idx_mutle_guest_retention ON mutle_game (user_id, created_at);
