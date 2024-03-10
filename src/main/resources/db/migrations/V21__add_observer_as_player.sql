ALTER TABLE
    `players`
    MODIFY COLUMN
        `Role` enum (
        'ATTACKER',
        'DEFENDER',
        'PLAYER',
        'OBSERVER'
        );

INSERT INTO `players` (Game_ID, User_ID, Role, Points)
SELECT ID, Creator_ID, 'OBSERVER', 0
FROM `games`
WHERE (Mode = 'PARTY' OR Mode = 'MELEE')
  AND (SELECT COUNT(*) FROM `players` WHERE Game_ID = games.ID AND games.Creator_ID = User_ID) = 0
  AND ID <> -1;
