CREATE OR REPLACE VIEW `view_playable_classes` AS
SELECT *
FROM `classes`
WHERE Puzzle = 0;

CREATE OR REPLACE VIEW `view_puzzle_classes` AS
SELECT *
FROM `classes`
WHERE Puzzle = 1;

CREATE OR REPLACE VIEW `view_battleground_games` AS
SELECT *
FROM games
WHERE Mode = 'PARTY';

CREATE OR REPLACE VIEW `view_puzzle_games` AS
SELECT *
FROM games
WHERE Mode = 'PUZZLE';

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

CREATE OR REPLACE VIEW `view_players_with_userdata` AS
SELECT p.*,
       u.Password  AS usersPassword,
       u.Username  AS usersUsername,
       u.Email     AS usersEmail,
       u.Validated AS usersValidated,
       u.Active    AS usersActive
FROM players AS p,
     users AS u
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

CREATE OR REPLACE VIEW `view_valid_users`
  AS SELECT * FROM `users`
    WHERE `User_ID` >= 5;

CREATE OR REPLACE VIEW `view_leaderboard`
  AS
    SELECT
      U.username                            AS username,
      IFNULL(NMutants, 0)                   AS NMutants,
      IFNULL(AScore, 0)                     AS AScore,
      IFNULL(NTests, 0)                     AS NTests,
      IFNULL(DScore, 0)                     AS DScore,
      IFNULL(NKilled, 0)                    AS NKilled,
      IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore
    FROM view_valid_users U
      LEFT JOIN view_attackers ON U.user_id = view_attackers.user_id
      LEFT JOIN view_defenders ON U.user_id = view_defenders.user_id;
