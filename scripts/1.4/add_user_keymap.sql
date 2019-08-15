/* Add "AllowContact" column to users. */
ALTER TABLE users ADD KeyMap enum('DEFAULT','SUBLIME','VIM','EMACS') NOT NULL DEFAULT 'DEFAULT';

/* Replace user view to include added 'KeyMap' column */
CREATE OR REPLACE VIEW `view_valid_users` AS
SELECT * FROM `users`
WHERE `User_ID` >= 5
  AND Active = 1;

/* Replace players with user data view */
CREATE OR REPLACE VIEW `view_players_with_userdata` AS
SELECT p.*,
       u.Password     AS usersPassword,
       u.Username     AS usersUsername,
       u.Email        AS usersEmail,
       u.Validated    AS usersValidated,
       u.Active       AS usersActive,
       u.AllowContact AS usersAllowContact,
       u.KeyMap       AS usersKeyMap
FROM players AS p,
     users   AS u
WHERE p.User_ID = u.User_ID;
