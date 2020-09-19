/* Add google truth flag to puzzle games */
ALTER TABLE `puzzles` ADD `Force_GoogleTruth` tinyint(1) DEFAULT '0'; 

/* Update game views */
CREATE OR REPLACE VIEW `view_battleground_games` AS
SELECT games.*, classes.Name, classes.JavaFile, classes.ClassFile, classes.Alias, classes.RequireMocking, classes.Active
FROM games,
     classes
WHERE Mode = 'PARTY'
  AND games.Class_ID = classes.Class_ID;

CREATE OR REPLACE VIEW `view_puzzle_games` AS
SELECT games.*, classes.Name, classes.JavaFile, classes.ClassFile, classes.Alias, classes.RequireMocking, classes.Active
FROM games,
     classes
WHERE Mode = 'PUZZLE'
  AND games.Class_ID = classes.Class_ID;
  
CREATE OR REPLACE VIEW `view_melee_games` AS
SELECT games.*,
       classes.Name,
       classes.JavaFile,
       classes.ClassFile,
       classes.Alias,
       classes.RequireMocking,
       classes.TestingFramework,
       classes.AssertionLibrary,
       classes.Active,
       classes.Puzzle,
       classes.Parent_Class
FROM games,
     classes
WHERE Mode = 'MELEE'
  AND games.Class_ID = classes.Class_ID;
