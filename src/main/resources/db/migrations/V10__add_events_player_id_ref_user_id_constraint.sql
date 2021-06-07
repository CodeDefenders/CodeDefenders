UPDATE events e
SET e.Player_ID = (
    WITH brokenEvents AS (
        SELECT Event_ID, Game_ID, Player_ID
        FROM events
                 LEFT OUTER JOIN users ON Player_ID = User_ID
        WHERE User_ID IS NULL
    ),
         mappedUserIDs AS (
             SELECT Event_ID, brokenEvents.Game_ID AS Game_ID, Player_ID, User_ID
             FROM brokenEvents
                      LEFT JOIN players
                                ON brokenEvents.Player_ID = players.ID AND
                                   brokenEvents.Game_ID = players.Game_ID
         )
    SELECT mappedUserIDs.User_ID
    FROM mappedUserIDs
    WHERE mappedUserIDs.Event_ID = e.Event_ID
)
WHERE e.Event_ID IN (
    WITH brokenEvents AS (
        SELECT Event_ID, Game_ID, Player_ID
        FROM events
                 LEFT OUTER JOIN users ON Player_ID = User_ID
        WHERE User_ID IS NULL
    ),
         mappedUserIDs AS (
             SELECT Event_ID, brokenEvents.Game_ID AS Game_ID, Player_ID, User_ID
             FROM brokenEvents
                      LEFT JOIN players
                                ON brokenEvents.Player_ID = players.ID AND
                                   brokenEvents.Game_ID = players.Game_ID
         )
    SELECT Event_ID
    FROM mappedUserIDs
);


ALTER TABLE `events`
    ADD CONSTRAINT FOREIGN KEY (Player_ID) REFERENCES `users` (User_ID);
