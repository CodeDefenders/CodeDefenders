# Update achievement level for the puzzle achievement.
UPDATE `has_achievement`
SET `Achievement_Level` = (SELECT MAX(`Level`)
                           FROM `achievements`
                           WHERE `ID` = 19
                             AND `Metric` <= `has_achievement`.`Metric`)
WHERE `Achievement_ID` = 19;
