DROP TABLE IF EXISTS has_achievement;
DROP TABLE IF EXISTS achievements;

CREATE TABLE achievements
(
    `ID`     INT          NOT NULL,
    `Level`  INT          NOT NULL,
    `Name`   VARCHAR(255) NOT NULL,
    `Metric` INT          NOT NULL,
    PRIMARY KEY (`ID`, `Level`)
);

CREATE TABLE has_achievement
(
    `Achievement_ID`     INT NOT NULL,
    `Achievement_level`  INT NOT NULL,
    `User_ID`            INT REFERENCES `users` (`User_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
    `Notification_Shown` BOOL DEFAULT FALSE,
    CONSTRAINT `achievement_user` FOREIGN KEY (`Achievement_ID`, `Achievement_level`) REFERENCES `achievements` (`ID`, `level`) ON DELETE NO ACTION ON UPDATE NO ACTION
);