-- MySQL dump 10.13  Distrib 5.7.9, for Win64 (x86_64)
--
-- Host: localhost    Database: codedefenders
-- ------------------------------------------------------
-- Server version	5.7.11-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

USE codedefenders;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `classes` (
  `Class_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Name` varchar(255) NOT NULL,
  `JavaFile` varchar(255) NOT NULL,
  `ClassFile` varchar(255) NOT NULL,
  `Alias` varchar(50) NOT NULL,
  PRIMARY KEY (`Class_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=221 DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX classes_Alias_uindex ON classes (Alias);
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `games` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `Class_ID` int(11) DEFAULT NULL,
  `Level` enum('EASY','MEDIUM','HARD') DEFAULT NULL,
  `Timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `Creator_ID` int(11) DEFAULT NULL,
  `Prize` int(11) DEFAULT NULL,
  `Defender_Value` int(11) DEFAULT '100',
  `Attacker_Value` int(11) DEFAULT '100',
  `Coverage_Goal` float DEFAULT NULL,
  `Mutant_Goal` float DEFAULT NULL,
  `Attackers_Needed` int(11) DEFAULT '0',
  `Defenders_Needed` int(11) DEFAULT '0',
  `Start_Time` TIMESTAMP DEFAULT 0,
  `Finish_Time` TIMESTAMP DEFAULT 0,
  `Attackers_Limit` int(11) DEFAULT '0',
  `Defenders_Limit` int(11) DEFAULT '0',
  `State` enum('CREATED','ACTIVE','FINISHED','GRACE_ONE','GRACE_TWO') DEFAULT 'CREATED',
  `CurrentRound` tinyint(4) NOT NULL DEFAULT '1',
  `FinalRound` tinyint(4) NOT NULL DEFAULT '5',
  `ActiveRole` enum('ATTACKER','DEFENDER') NOT NULL DEFAULT 'ATTACKER',
  `Mode` enum('SINGLE','DUEL','PARTY','UTESTING') NOT NULL DEFAULT 'DUEL',
  PRIMARY KEY (`ID`),
  KEY `fk_creatorId_idx` (`Creator_ID`),
  KEY `fk_className_idx` (`Class_ID`),
  CONSTRAINT `fk_classId` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_className` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_creatorId` FOREIGN KEY (`Creator_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mutants`
--

DROP TABLE IF EXISTS `mutants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mutants` (
  `Mutant_ID` int(11) NOT NULL AUTO_INCREMENT,
  `JavaFile` varchar(255) NOT NULL,
  `ClassFile` varchar(255) DEFAULT NULL,
  `Alive` tinyint(1) NOT NULL DEFAULT '1',
  `Game_ID` int(11) NOT NULL,
  `RoundCreated` int(11) NOT NULL,
  `RoundKilled` int(11) DEFAULT NULL,
  `Equivalent` enum('ASSUMED_NO','PENDING_TEST','DECLARED_YES','ASSUMED_YES','PROVEN_NO') NOT NULL,
  `Player_ID` int(11) DEFAULT NULL,
  `NumberAiKillingTests` int(11) DEFAULT '0',
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Points` int(11) DEFAULT '0',
  PRIMARY KEY (`Mutant_ID`),
  KEY `fk_gameId_idx` (`Game_ID`),
  KEY `fk_playerId_idx` (`Player_ID`),
  CONSTRAINT `fk_gameId_muts` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_playerId_muts` FOREIGN KEY (`Player_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `players`
--

DROP TABLE IF EXISTS `players`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `players` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `User_ID` int(11) NOT NULL,
  `Game_ID` int(11) NOT NULL,
  `Points` int(11) NOT NULL,
  `Role` enum('ATTACKER','DEFENDER') NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `fk_userId_players_idx` (`User_ID`),
  KEY `fk_gameId_players_idx` (`Game_ID`),
  CONSTRAINT `fk_gameId_players` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_userId_players` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `targetexecutions`
--

DROP TABLE IF EXISTS `targetexecutions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `targetexecutions` (
  `TargetExecution_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Test_ID` int(11) DEFAULT NULL,
  `Mutant_ID` int(11) DEFAULT NULL,
  `Target` enum('COMPILE_MUTANT','COMPILE_TEST','TEST_ORIGINAL','TEST_MUTANT','TEST_EQUIVALENCE') DEFAULT NULL,
  `Status` enum('SUCCESS','FAIL','ERROR') NOT NULL,
  `Message` varchar(2000) DEFAULT NULL,
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`TargetExecution_ID`),
  KEY `Test_ID` (`Test_ID`),
  KEY `Mutant_ID` (`Mutant_ID`),
  CONSTRAINT `targetexecutions_ibfk_1` FOREIGN KEY (`Test_ID`) REFERENCES `tests` (`Test_ID`),
  CONSTRAINT `targetexecutions_ibfk_2` FOREIGN KEY (`Mutant_ID`) REFERENCES `mutants` (`Mutant_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=614 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tests`
--

DROP TABLE IF EXISTS `tests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tests` (
  `Test_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Game_ID` int(11) NOT NULL,
  `JavaFile` varchar(255) NOT NULL,
  `ClassFile` varchar(255) DEFAULT NULL,
  `RoundCreated` int(11) NOT NULL,
  `MutantsKilled` int(11) DEFAULT '0',
  `Player_ID` int(11) NOT NULL,
  `NumberAiMutantsKilled` int(11) DEFAULT '0',
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Lines_Covered` longtext,
  `Lines_Uncovered` longtext,
  `Points` int(11) DEFAULT '0',
  PRIMARY KEY (`Test_ID`),
  KEY `fk_playerId_idx` (`Player_ID`),
  KEY `fk_gameId_tests_idx` (`Game_ID`),
  KEY `fk_playerId_tests_idx` (`Player_ID`),
  CONSTRAINT `fk_gameId_tests` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_playerId_tests` FOREIGN KEY (`Player_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=194 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usedaimutants`
--

DROP TABLE IF EXISTS `usedaimutants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usedaimutants` (
  `UsedMutant_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Value` int(11) DEFAULT NULL,
  `Game_ID` int(11) NOT NULL,
  PRIMARY KEY (`UsedMutant_ID`),
  KEY `fk_gameId_ai_mutants_idx` (`Game_ID`),
  CONSTRAINT `fk_gameId_ai_mutants` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `usedaitests`
--

DROP TABLE IF EXISTS `usedaitests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `usedaitests` (
  `UsedTest_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Value` int(11) DEFAULT NULL,
  `Game_ID` int(11) NOT NULL,
  PRIMARY KEY (`UsedTest_ID`),
  KEY `fk_gameId_ai_test_idx` (`Game_ID`),
  CONSTRAINT `fk_gameId_ai_test` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `User_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Username` varchar(20) NOT NULL,
  `Password` char(60) NOT NULL,
  `Email` varchar(320) NOT NULL,
  PRIMARY KEY (`User_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX users_email_index ON users (Email);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

--
-- Table structure for table `sessions`
--

DROP TABLE IF EXISTS `sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sessions` (
  `Session_ID` int(11) NOT NULL AUTO_INCREMENT,
  `User_ID` int(11) NOT NULL,
  `IP_Address` varchar(320) NOT NULL,
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`Session_ID`),
  CONSTRAINT `fk_userId_sessions` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=194 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

DROP TABLE IF EXISTS `equivalences`;
CREATE TABLE `equivalences` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `Mutant_ID` int(11) DEFAULT NULL,
  `Defender_ID` int(11) DEFAULT NULL,
  `Mutant_Points` int(11) DEFAULT '0',
  `Expired` TINYINT(4) DEFAULT '0' NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `ID_UNIQUE` (`ID`),
  KEY `fk_equiv_def_idx` (`Defender_ID`),
  KEY `fk_equiv_mutant_idx` (`Mutant_ID`),
  CONSTRAINT `fk_equiv_def` FOREIGN KEY (`Defender_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_equiv_mutant` FOREIGN KEY (`Mutant_ID`) REFERENCES `mutants` (`Mutant_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

-- Dump completed on 2016-07-13 11:18:20

INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`) VALUES (1, 'Mutator', 'AI_ATTACKER_INACCESSIBLE', 'codedef_mutator@sheffield.ac.uk');
INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`) VALUES (2, 'TestGen', 'AI_DEFENDER_INACCESSIBLE', 'codedef_testgen@sheffield.ac.uk');

-- Event to activate multiplayer game
SET @@global.event_scheduler = 1;
-- HANDLING OF EQUIVALENCES AFTER TIME EXPIRATION

DROP PROCEDURE IF EXISTS proc_multiplayer_task;

DELIMITER //
CREATE PROCEDURE proc_multiplayer_task()
BEGIN
  UPDATE games SET State='ACTIVE'
  WHERE Mode='PARTY' AND State='CREATED' AND Start_Time<=CURRENT_TIMESTAMP;

  UPDATE games SET State='GRACE_ONE'
  WHERE Mode='PARTY' AND State='ACTIVE' AND Finish_Time<=DATE_ADD(NOW(), INTERVAL 1 HOUR);

  UPDATE games SET State='GRACE_TWO'
  WHERE Mode='PARTY' AND State='GRACE_ONE' AND Finish_Time<=DATE_ADD(NOW(), INTERVAL 45 MINUTE);

  UPDATE games AS g
  LEFT JOIN mutants AS m ON m.Game_ID = g.ID
  LEFT JOIN equivalences AS e ON e.Mutant_ID = m.Mutant_ID
  SET State='FINISHED', e.Expired = 1
  WHERE Mode='PARTY' AND State='GRACE_TWO' AND Finish_Time<=NOW();

  UPDATE equivalences AS e
  LEFT JOIN mutants AS m ON e.Mutant_ID = m.Mutant_ID
  SET e.Expired = 0 WHERE e.Expired = 1 AND m.Equivalent != 'PENDING_TEST';

  UPDATE mutants AS m
  LEFT JOIN equivalences AS e ON e.Mutant_ID = m.Mutant_ID
  SET m.Points = 0, m.Equivalent = 'ASSUMED_YES' WHERE e.Expired = 1;

   UPDATE players AS p
 LEFT JOIN equivalences AS ee ON ee.Defender_ID = p.ID
 SET p.Points = p.Points + (SELECT COUNT(e.ID)
         FROM equivalences AS e
        WHERE ee.Defender_ID = e.Defender_ID AND
        e.Expired = 1
        GROUP BY ee.Defender_ID
       )
  WHERE ee.Expired = 1;

  UPDATE equivalences SET Expired = 0
  WHERE Expired = 1;
END //
DELIMITER ;

DROP EVENT IF EXISTS event_mp_task;
CREATE EVENT IF NOT EXISTS event_mp_task
  ON SCHEDULE EVERY 1 MINUTE
  ON COMPLETION PRESERVE
DO
  CALL proc_multiplayer_task();