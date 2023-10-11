CREATE TABLE achievements
(
    `ID`           INT          NOT NULL,
    `Level`        INT          NOT NULL,
    `Index` INT NOT NULL,
    `Name`         VARCHAR(255) NOT NULL,
    `Description`  VARCHAR(255) NOT NULL,
    `ProgressText` VARCHAR(255) NOT NULL,
    `Metric`       INT          NOT NULL,
    PRIMARY KEY (`ID`, `Level`)
);

CREATE TABLE has_achievement
(
    `Achievement_ID` INT NOT NULL,
    `Achievement_level` INT DEFAULT 0,
    `User_ID`        INT REFERENCES `users` (`User_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    `Metric`            INT NOT NULL,
    PRIMARY KEY (`Achievement_ID`, `User_ID`),
    CONSTRAINT `achievement_user` FOREIGN KEY (`Achievement_ID`, `Achievement_level`) REFERENCES `achievements` (`ID`, `level`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

INSERT INTO achievements
VALUES (0, 0, 0, 'No games played yet', 'Play your first game to unlock this achievement', '{0} of {1} games played',
        0),
       (0, 1, 0, 'Newbie', 'Play your first game!', '{0} of {1} games played to reach the next level', 1),
       (0, 2, 0, 'Bronze Player', 'Play {0} games', '{0} of {1} games played to reach the next level', 3),
       (0, 3, 0, 'Silver Player', 'Play {0} games', '{0} of {1} games played to reach the next level', 10),
       (0, 4, 0, 'Gold Player', 'Play {0} games', '{0} games played, max level reached', 50),

       (1, 0, 1, 'No game played as attacker yet', 'Play as attacker to unlock this achievement',
        '{0} of {1} games played', 0),
       (1, 1, 1, 'Prepare to Attack', 'Play your first multiplayer game as attacker',
        '{0} of {1} games played to reach the next level', 1),
       (1, 2, 1, 'Bronze Attacker', 'Play {0} games as attacker', '{0} of {1} games played to reach the next level', 3),
       (1, 3, 1, 'Silver Attacker', 'Play {0} games as attacker', '{0} of {1} games played to reach the next level',
        10),
       (1, 4, 1, 'Gold Attacker', 'Play {0} games as attacker', '{0} games played, max level reached', 50),

       (2, 0, 2, 'No game played as defender yet', 'Play as defender to unlock this achievement',
        '{0} of {1} games played', 0),
       (2, 1, 2, 'Prepare Your Defenses', 'Play your first multiplayer game as defender',
        '{0} of {1} games played to reach the next level', 1),
       (2, 2, 2, 'Bronze Defender', 'Play {0} games as defender', '{0} of {1} games played to reach the next level', 3),
       (2, 3, 2, 'Silver Defender', 'Play {0} games as defender', '{0} of {1} games played to reach the next level',
        10),
       (2, 4, 2, 'Gold Defender', 'Play {0} games as defender', '{0} games played, max level reached', 50),

       (3, 0, 3, 'No melee game played yet', 'Try the melee mode to unlock this achievement', '{0} of {1} games played',
        0),
       (3, 1, 3, 'Melee Starter', 'Play your first melee game', '{0} of {1} games played to reach the next level', 1),
       (3, 2, 3, 'Melee Bronze', 'Play {0} melee games', '{0} of {1} games played to reach the next level', 3),
       (3, 3, 3, 'Melee Silver', 'Play {0} melee games', '{0} of {1} games played to reach the next level', 10),
       (3, 4, 3, 'Melee Gold', 'Play {0} melee games', '{0} games played, max level reached', 50),

       (4, 0, 8, 'No tests written yet', 'Write tests to unlock this achievement', '{0} of {1} test written', 0),
       (4, 1, 8, 'The First Test', 'Write your first test', '{0} of {1} tests written to reach the next level', 1),
       (4, 2, 8, 'Bronze Test Writer', 'Write {0} tests', '{0} of {1} tests written to reach the next level', 10),
       (4, 3, 8, 'Silver Test Writer', 'Write {0} tests', '{0} of {1} tests written to reach the next level', 50),
       (4, 4, 8, 'Gold Test Writer', 'Write {0} tests', '{0} tests written, max level reached', 200),

       (5, 0, 9, 'No mutants created yet', 'Create mutants to unlock this achievement', '{0} of {1} mutant created', 0),
       (5, 1, 9, 'The First Mutant', 'Create your first mutant', '{0} of {1} mutants created to reach the next level',
        1),
       (5, 2, 9, 'Bronze Mutant Creator', 'Create {0} mutants', '{0} of {1} mutants created to reach the next level',
        10),
       (5, 3, 9, 'Silver Mutant Creator', 'Create {0} mutants', '{0} of {1} mutants created to reach the next level',
        50),
       (5, 4, 9, 'Gold Mutant Creator', 'Create {0} mutants', '{0} mutants created, max level reached', 200),

       (6, 0, 4, 'No games won', 'Win games to unlock this achievement', '{0} of {1} games won', 0),
       (6, 1, 4, 'First Victory', 'Win your first game', '{0} of {1} games won to reach the next level', 1),
       (6, 2, 4, 'Bronze Winner', 'Win {0} games', '{0} of {1} games won to reach the next level', 3),
       (6, 3, 4, 'Silver Winner', 'Win {0} games', '{0} of {1} games won to reach the next level', 10),
       (6, 4, 4, 'Gold Winner', 'Win {0} games', '{0} games won, max level reached', 50),

       (7, 0, 5, 'No games won as attacker', 'Win games as attacker to unlock this achievement', '{0} of {1} games won',
        0),
       (7, 1, 5, 'First Attacker Victory', 'Win your first game as attacker',
        '{0} of {1} games won to reach the next level', 1),
       (7, 2, 5, 'Good Attacker', 'Win {0} games as attacker', '{0} of {1} games won to reach the next level', 3),
       (7, 3, 5, 'Advanced Attacker', 'Win {0} games as attacker', '{0} of {1} games won to reach the next level', 8),
       (7, 4, 5, 'Master Attacker', 'Win {0} games as attacker', '{0} games won, max level reached', 25),

       (8, 0, 6, 'No games won as defender', 'Win games as defender to unlock this achievement', '{0} of {1} games won',
        0),
       (8, 1, 6, 'First Defender Victory', 'Win your first game as defender',
        '{0} of {1} games won to reach the next level', 1),
       (8, 2, 6, 'Good Defender', 'Win {0} games as defender', '{0} of {1} games won to reach the next level', 3),
       (8, 3, 6, 'Advanced Defender', 'Win {0} games as defender', '{0} of {1} games won to reach the next level', 8),
       (8, 4, 6, 'Master Defender', 'Win {0} games as defender', '{0} games won, max level reached', 25),

       (9, 0, 7, 'No puzzles solved yet', 'Solve puzzles to unlock this achievement', '{0} of {1} puzzles solved', 0),
       (9, 1, 7, 'The First Puzzle', 'Solve your first puzzle', '{0} of {1} puzzles solved to reach the next level', 1),
       (9, 2, 7, 'Bronze Puzzle Solver', 'Solve {0} puzzles', '{0} of {1} puzzles solved to reach the next level', 5),
       (9, 3, 7, 'Silver Puzzle Solver', 'Solve {0} puzzles', '{0} of {1} puzzles solved to reach the next level', 15),
       (9, 4, 7, 'Puzzle Expert', 'Solve {0} puzzles', '{0} puzzles solved, max level reached', 25);


# Update the metric of the puzzle achievement according to the number of solved puzzles.
INSERT INTO `has_achievement` (Achievement_ID, User_ID, Metric)
SELECT 9, Creator_ID AS user, COUNT(*) AS count
FROM `games`
WHERE Mode = 'PUZZLE'
  AND State = 'SOLVED'
GROUP BY Creator_ID
ON DUPLICATE KEY UPDATE Metric = VALUES(Metric);

# Update achievement level for the puzzle achievement.
UPDATE `has_achievement`
SET `Achievement_Level` = (SELECT MAX(`Level`)
                           FROM `achievements`
                           WHERE `ID` = 9
                             AND `Metric` <= `has_achievement`.`Metric`)
WHERE `Achievement_ID` = 9;