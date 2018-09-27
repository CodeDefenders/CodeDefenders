-- Update dummy attacker and dummy defender users, since you cannot create folders with the old IDs -2 and -3
DELETE FROM players
where User_ID IN (-2, -3);

UPDATE users
SET User_ID = 3
WHERE User_ID = -2;

UPDATE users
SET User_ID = 4
WHERE User_ID = -3;

INSERT INTO `players` (`ID`, `User_ID`, `Game_ID`, `Points`, `Role`, `Active`)
VALUES (3, 3, -1, 0, 'ATTACKER', 0),
       (4, 4, -1, 0, 'DEFENDER', 0);
