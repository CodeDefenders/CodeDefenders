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


-- Mutant views

-- Mutants with User Info
CREATE OR REPLACE VIEW `view_mutants_with_user` AS
SELECT mutants.*, users.*
FROM mutants
     LEFT JOIN players ON players.ID = mutants.Player_ID
     LEFT JOIN users ON players.User_ID = users.User_ID;

-- Valid mutants created by users
CREATE OR REPLACE VIEW `view_valid_user_mutants` AS
SELECT mutants.*
FROM view_mutants_with_user mutants, players
WHERE mutants.ClassFile IS NOT NULL     -- Only valid mutants
  AND mutants.Player_ID = players.ID
  AND players.ID <> -1                  -- Exclude templates of predefined mutants
  AND players.User_ID > 4;              -- Exclude instances of predefined mutants

-- Templates of predefined mutants, also stored to the mutants table
CREATE OR REPLACE VIEW `view_system_mutant_templates` AS
SELECT mutants.*
FROM view_mutants_with_user mutants
WHERE mutants.ClassFile IS NOT NULL     -- Only valid mutants
  AND Player_ID = -1;                   -- Only templates of predefined mutants

-- Instances of predefined mutants in games
CREATE OR REPLACE VIEW `view_system_mutant_instances` AS
SELECT mutants.*
FROM view_mutants_with_user mutants, players
WHERE mutants.ClassFile IS NOT NULL     -- Only valid mutants
  AND mutants.Player_ID = players.ID
  AND players.User_ID = 3;              -- Only instances of predefined mutants

-- Mutants as they appear in games
CREATE OR REPLACE VIEW `view_valid_game_mutants` AS
    SELECT * FROM view_valid_user_mutants
    UNION ALL
    SELECT * FROM view_system_mutant_instances;


-- Test views

-- Tests with User Info
CREATE OR REPLACE VIEW `view_tests_with_user` AS
SELECT tests.*, users.*
FROM tests
     LEFT JOIN players ON players.ID = tests.Player_ID
     LEFT JOIN users ON players.User_ID = users.User_ID;

-- Valid tests created by users
CREATE OR REPLACE VIEW `view_valid_user_tests` AS
SELECT tests.*
FROM view_tests_with_user tests, players
WHERE tests.ClassFile IS NOT NULL
  AND EXISTS(
    SELECT *
    FROM targetexecutions ex
    WHERE ex.Test_ID = tests.Test_ID
      AND ex.Target = 'TEST_ORIGINAL'   -- Note: TEST_ORIGINAL execution does not exist for predefined tests.
      AND ex.Status = 'SUCCESS'         -- We still filter by player/user ID to be sure though.
  )
  AND tests.Player_ID = players.ID
  AND players.ID <> -1      -- Exclude original predefined tests
  AND players.User_ID > 4;  -- Exclude instances of predefined tests

-- Templates of predefined tests, also stored to the tests table
CREATE OR REPLACE VIEW `view_system_test_templates` AS
SELECT tests.*
FROM view_tests_with_user tests
WHERE ClassFile IS NOT NULL
  AND Player_ID = -1;       -- Only original predefined tests

-- Instances of predefined tests in games
CREATE OR REPLACE VIEW `view_system_test_instances` AS
SELECT tests.*
FROM view_tests_with_user tests, players
WHERE ClassFile IS NOT NULL
  AND tests.Player_ID = players.ID
  AND players.User_ID = 4;  -- Only instances of predefined tests

-- Tests as they appear in games
CREATE OR REPLACE VIEW `view_valid_game_tests` AS
    SELECT * FROM view_valid_user_tests
    UNION ALL
    SELECT * FROM view_system_test_instances;


-- Drop old views

DROP VIEW IF EXISTS `view_valid_mutants`;
DROP VIEW IF EXISTS `view_valid_tests`;
