DROP TABLE IF EXISTS `killmapjob`;

CREATE TABLE killmapjob
(
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `Game_ID` int(11),
  `Class_ID` int(11),
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`)
) AUTO_INCREMENT=1;

