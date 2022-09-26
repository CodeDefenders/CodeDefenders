INSERT INTO `settings` (name,            type,        STRING_VALUE, INT_VALUE, BOOL_VALUE)
VALUES ('GAME_DURATION_MINUTES_MAX',     'INT_VALUE', NULL,         10080,     NULL      ),
       ('GAME_DURATION_MINUTES_DEFAULT', 'INT_VALUE', NULL,            60,     NULL      );

ALTER TABLE `games`
ADD COLUMN IF NOT EXISTS `Game_Duration_Minutes` int(11) DEFAULT NULL;

UPDATE `games`
SET `Game_Duration_Minutes` = (
    SELECT `settings`.INT_VALUE
    FROM `settings`
    WHERE `settings`.name = 'GAME_DURATION_MINUTES_DEFAULT'
)
WHERE `Game_Duration_Minutes` IS NULL;

ALTER TABLE `games`
ALTER Start_Time
SET DEFAULT (CURRENT_TIMESTAMP());