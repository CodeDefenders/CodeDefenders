# Update metrics for solved puzzles
INSERT INTO has_achievement (Achievement_ID, User_ID, Metric)
SELECT 19, Creator_ID as user, count(*) as SolvedPuzzles
FROM games
WHERE Mode = 'PUZZLE'
  AND State = 'SOLVED'
  AND CurrentRound = 1
GROUP BY Creator_ID
ON DUPLICATE KEY UPDATE Metric = VALUES(Metric);
