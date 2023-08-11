DROP TABLE IF EXISTS has_achievement;
DROP TABLE IF EXISTS achievements;

CREATE TABLE achievements
(
    `ID`          INT          NOT NULL,
    `Level`       INT          NOT NULL,
    `Name`        VARCHAR(255) NOT NULL,
    `Description` VARCHAR(255) NOT NULL,
    `Metric`      INT          NOT NULL,
    PRIMARY KEY (`ID`, `Level`)
);

CREATE TABLE has_achievement
(
    `Achievement_ID`     INT NOT NULL,
    `Achievement_level` INT DEFAULT 0,
    `User_ID`            INT REFERENCES `users` (`User_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    `Notification_Shown` BOOL DEFAULT FALSE,
    `Metric`            INT NOT NULL,
    PRIMARY KEY (`Achievement_ID`, `User_ID`)
    #, CONSTRAINT `achievement_user` FOREIGN KEY (`Achievement_ID`, `Achievement_level`) REFERENCES `achievements` (`ID`, `level`) ON DELETE NO ACTION ON UPDATE NO ACTION
);

INSERT INTO achievements
VALUES (0, 1, 'Newbie', 'Play your first game!', 1),
       (0, 2, 'Bronze Player', 'Play {0} games', 3),
       (0, 3, 'Silver Player', 'Play {0} games', 10),
       (0, 4, 'Gold Player', 'Play {0} games', 50),

       (1, 1, 'Prepare to Attack', 'Play your first multiplayer game as attacker', 1),
       (1, 2, 'Bronze Attacker', 'Play {0} games as attacker', 3),
       (1, 3, 'Silver Attacker', 'Play {0} games as attacker', 10),
       (1, 4, 'Gold Attacker', 'Play {0} games as attacker', 50),

       (2, 1, 'Prepare Your Defenses', 'Play your first multiplayer game as defender', 1),
       (2, 2, 'Bronze Defender', 'Play {0} games as defender', 3),
       (2, 3, 'Silver Defender', 'Play {0} games as defender', 10),
       (2, 4, 'Gold Defender', 'Play {0} games as defender', 50),

       (3, 1, 'Melee Starter', 'Play your first melee game', 1),
       (3, 2, 'Melee Bronze', 'Play {0} melee games', 3),
       (3, 3, 'Melee Silver', 'Play {0} melee games', 10),
       (3, 4, 'Melee Gold', 'Play {0} melee games', 50),

       (4, 1, 'The First Test', 'Write your first test', 1),
       (4, 2, 'Bronze Test Writer', 'Write {0} tests', 10),
       (4, 3, 'Silver Test Writer', 'Write {0} tests', 50),
       (4, 4, 'Gold Test Writer', 'Write {0} tests', 200),

       (5, 1, 'The First Mutant', 'Create your first mutant', 1),
       (5, 2, 'Bronze Mutant Creator', 'Create {0} mutants', 10),
       (5, 3, 'Silver Mutant Creator', 'Create {0} mutants', 50),
       (5, 4, 'Gold Mutant Creator', 'Create {0} mutants', 200);
