--
-- The Views need to be updated everytime as we use * in the SELECT and don't hardcode the columns
-- So we use the flyway timestamp to always recreate the views regardless of changes
-- ${flyway:timestamp}
-- More details here: https://flywaydb.org/blog/flyway-timestampsAndRepeatables


CREATE OR REPLACE VIEW `view_active_classes` AS
SELECT *
FROM classes
WHERE Active = 1;

CREATE OR REPLACE VIEW `view_playable_classes` AS
SELECT *
FROM `view_active_classes`
WHERE Puzzle = 0;

CREATE OR REPLACE VIEW `view_puzzle_classes` AS
SELECT *
FROM `view_active_classes`
WHERE Puzzle = 1;

CREATE OR REPLACE VIEW `view_active_puzzles` AS
SELECT *
FROM `puzzles`
WHERE Active = 1;

CREATE OR REPLACE VIEW `view_battleground_games` AS
SELECT games.*, UNIX_TIMESTAMP(games.Start_Time) AS `Timestamp_Start`, classes.Name, classes.JavaFile, classes.ClassFile, classes.Alias, classes.RequireMocking, classes.TestingFramework, classes.AssertionLibrary, classes.Active, classes.Puzzle, classes.Parent_Class
FROM games,
     classes
WHERE Mode = 'PARTY'
  AND games.Class_ID = classes.Class_ID;

CREATE OR REPLACE VIEW `view_melee_games` AS
SELECT games.*, UNIX_TIMESTAMP(games.Start_Time) AS `Timestamp_Start`, classes.Name, classes.JavaFile, classes.ClassFile, classes.Alias, classes.RequireMocking, classes.TestingFramework, classes.AssertionLibrary, classes.Active, classes.Puzzle, classes.Parent_Class
FROM games,
     classes
WHERE Mode = 'MELEE'
  AND games.Class_ID = classes.Class_ID;


CREATE OR REPLACE VIEW `view_puzzle_games` AS
SELECT games.*, classes.Name, classes.JavaFile, classes.ClassFile, classes.Alias, classes.RequireMocking, classes.TestingFramework, classes.AssertionLibrary, classes.Active, classes.Puzzle, classes.Parent_Class
FROM games,
     classes
WHERE Mode = 'PUZZLE'
  AND games.Class_ID = classes.Class_ID;

CREATE OR REPLACE VIEW `view_mutants_with_user` AS
SELECT mutants.*, users.*
FROM mutants
         LEFT JOIN players ON players.ID = mutants.Player_ID
         LEFT JOIN users ON players.User_ID = users.User_ID;

CREATE OR REPLACE VIEW `view_valid_mutants` AS
SELECT *
FROM view_mutants_with_user
WHERE ClassFile IS NOT NULL;

CREATE OR REPLACE VIEW `view_players` AS
SELECT *
FROM players
WHERE `ID` >= 100;

CREATE OR REPLACE VIEW `view_valid_users` AS
SELECT * FROM `users`
WHERE `User_ID` >= 5
  AND Active = 1;

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
     view_valid_users AS u
WHERE p.User_ID = u.User_ID;

CREATE OR REPLACE VIEW `view_valid_tests` AS
SELECT *
FROM tests
WHERE tests.ClassFile IS NOT NULL
  AND EXISTS(
        SELECT *
        FROM targetexecutions ex
        WHERE ex.Test_ID = tests.Test_ID
          AND ex.Target = 'TEST_ORIGINAL'
          AND ex.Status = 'SUCCESS'
    );
