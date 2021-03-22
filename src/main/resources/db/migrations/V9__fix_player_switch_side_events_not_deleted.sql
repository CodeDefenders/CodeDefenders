UPDATE events e
SET e.Event_Status = 'DELETED'
WHERE e.Event_ID IN (
    WITH firstEvent AS (
        SELECT MIN(FIRSTJOINEDEVENT.Event_ID) AS Event_ID,
               FIRSTJOINEDEVENT.Game_ID,
               FIRSTJOINEDEVENT.Player_ID     AS User_ID
        FROM (
                 SELECT MAX(Event_ID) AS Event_ID, Game_ID, Player_ID
                 FROM events
                 WHERE (Event_Type = 'ATTACKER_JOINED' OR Event_Type = 'DEFENDER_JOINED')
                   AND Event_Status = 'GAME'
                 GROUP BY Game_ID, Player_ID
                 UNION
                 SELECT MAX(Event_ID), Game_ID, Player_ID
                 FROM events
                 WHERE (Event_Type = 'ATTACKER_JOINED' OR Event_Type = 'DEFENDER_JOINED')
                   AND Event_Status = 'NEW'
                 GROUP BY Game_ID, Player_ID
             ) as FIRSTJOINEDEVENT
        GROUP BY Game_ID, Player_ID
    )
    SELECT e.Event_ID
    FROM events e,
         firstEvent
    WHERE e.Event_Status <> 'DELETED'
      AND e.Event_Type NOT IN
          ('GAME_CREATED', 'GAME_STARTED', 'GAME_FINISHED', 'GAME_GRACE_ONE', 'GAME_GRACE_TWO', 'GAME_PLAYER_LEFT')
      AND e.Game_ID = firstEvent.Game_ID
      AND e.Player_ID = firstEvent.User_ID
      AND e.Event_ID < firstEvent.Event_ID
)
