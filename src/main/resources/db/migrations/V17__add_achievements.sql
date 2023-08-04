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
VALUES (0, 0, '', '', 0),
       (0, 1, 'Newbie', 'Play your first game!', 1),
       (0, 2, 'Bronze Player', 'Play {0} games', 3),
       (0, 3, 'Silver Player', 'Play {0} games', 10),
       (0, 4, 'Gold Player', 'Play {0} games', 50);
