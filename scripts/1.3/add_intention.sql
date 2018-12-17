-- Add new table to store intentions
DROP TABLE IF EXISTS `intention`;
CREATE TABLE `intention` (
  `Intention_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Test_ID` int(11),
  `Mutant_ID` int(11),
  `Game_ID` int(11) NOT NULL,
  `Target_Mutants` longtext,
  `Target_Lines` longtext,
  `Target_Mutant_Type` longtext,
   PRIMARY KEY (`Intention_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Include per Game configuration
ALTER TABLE games
ADD COLUMN CapturePlayersIntention TINYINT(1) NOT NULL DEFAULT 0;