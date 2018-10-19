--
-- Table structure for table 'dependencies'
--
DROP TABLE IF EXISTS `dependencies`;
CREATE TABLE `dependencies` (
  `Dependency_ID` int(11)      NOT NULL AUTO_INCREMENT,
  `Class_ID`      int(11)      NOT NULL,
  `JavaFile`      varchar(255) NOT NULL,
  `ClassFile`     varchar(255) NOT NULL,
  PRIMARY KEY (`Dependency_ID`),
  CONSTRAINT `fk_classId_dependencies` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;

