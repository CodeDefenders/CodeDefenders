/* Add "AllowContact" column to users. */
ALTER TABLE users ADD AllowContact TINYINT(1) DEFAULT 0 NOT NULL;

/* Replace players with user data view */
CREATE OR REPLACE VIEW `view_players_with_userdata` AS
SELECT p.*,
       u.Password     AS usersPassword,
       u.Username     AS usersUsername,
       u.Email        AS usersEmail,
       u.Validated    AS usersValidated,
       u.Active       AS usersActive,
       u.AllowContact AS usersAllowContact
FROM players AS p,
     users   AS u
WHERE p.User_ID = u.User_ID;
