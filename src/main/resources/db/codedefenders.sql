-- MySQL dump 10.13  Distrib 5.6.26, for Win64 (x86_64)
--
-- Host: localhost    Database: codedefenders
-- ------------------------------------------------------
-- Server version	5.6.26-log

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
  PRIMARY KEY (`Class_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `games` (
  `Game_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Attacker_ID` int(11) DEFAULT NULL,
  `Defender_ID` int(11) DEFAULT NULL,
  `CurrentRound` tinyint(4) NOT NULL DEFAULT '1',
  `FinalRound` tinyint(4) NOT NULL DEFAULT '5',
  `ActiveRole` enum('ATTACKER','DEFENDER') NOT NULL DEFAULT 'ATTACKER',
  `Class_ID` int(11) DEFAULT NULL,
  `State` enum('CREATED','ACTIVE','FINISHED') NOT NULL DEFAULT 'CREATED',
  `Level` enum('EASY','MEDIUM','HARD') NOT NULL,
  `Mode` enum('SINGLE','DUEL', 'PARTY', 'UTESTING') NOT NULL DEFAULT 'DUEL',
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`Game_ID`),
  KEY `Attacker_ID` (`Attacker_ID`),
  KEY `Defender_ID` (`Defender_ID`),
  KEY `Class_ID` (`Class_ID`),
  CONSTRAINT `games_ibfk_1` FOREIGN KEY (`Attacker_ID`) REFERENCES `users` (`User_ID`),
  CONSTRAINT `games_ibfk_2` FOREIGN KEY (`Defender_ID`) REFERENCES `users` (`User_ID`),
  CONSTRAINT `games_ibfk_3` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
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
  `ClassFile` varchar(255),
  `Alive` tinyint(1) NOT NULL DEFAULT '1',
  `Game_ID` int(11) NOT NULL,
  `RoundCreated` int(11) NOT NULL,
  `RoundKilled` int(11) DEFAULT NULL,
  `Equivalent` enum('ASSUMED_NO','PENDING_TEST','DECLARED_YES','ASSUMED_YES','PROVEN_NO') NOT NULL,
  `Owner_ID` int(11) NOT NULL,
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`Mutant_ID`),
  KEY `Game_ID` (`Game_ID`),
  KEY `Owner_ID` (`Owner_ID`),
  CONSTRAINT `mutants_ibfk_1` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`Game_ID`),
  CONSTRAINT `mutants_ibfk_2` FOREIGN KEY (`Owner_ID`) REFERENCES `users` (`User_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
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
  `ClassFile` varchar(255),
  `RoundCreated` int(11) NOT NULL,
  `MutantsKilled` int(11) DEFAULT '0',
  `Owner_ID` int(11) NOT NULL,
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`Test_ID`),
  KEY `Game_ID` (`Game_ID`),
  KEY `Owner_ID` (`Owner_ID`),
  CONSTRAINT `tests_ibfk_1` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`Game_ID`),
  CONSTRAINT `tests_ibfk_2` FOREIGN KEY (`Owner_ID`) REFERENCES `users` (`User_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
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
  PRIMARY KEY (`User_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-12-04 15:54:48

INSERT INTO `users` VALUES (1, 'AI', 'DUMMY_INACCESSIBLE');