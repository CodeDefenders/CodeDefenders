-- MySQL dump 10.13  Distrib 5.6.26, for Win64 (x86_64)
--
-- Host: localhost    Database: gammut
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
  `Name` varchar(20) NOT NULL,
  `JavaFile` varchar(255) NOT NULL,
  `ClassFile` varchar(255) NOT NULL,
  PRIMARY KEY (`Class_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
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
  `ActivePlayer` enum('ATTACKER','DEFENDER','NEITHER') NOT NULL DEFAULT 'NEITHER',
  `Class_ID` int(11) DEFAULT NULL,
  `State` enum('CREATED','IN PROGRESS','FINISHED') NOT NULL,
  PRIMARY KEY (`Game_ID`),
  KEY `Attacker_ID` (`Attacker_ID`),
  KEY `Defender_ID` (`Defender_ID`),
  KEY `Class_ID` (`Class_ID`),
  CONSTRAINT `games_ibfk_1` FOREIGN KEY (`Attacker_ID`) REFERENCES `users` (`User_ID`),
  CONSTRAINT `games_ibfk_2` FOREIGN KEY (`Defender_ID`) REFERENCES `users` (`User_ID`),
  CONSTRAINT `games_ibfk_3` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8;
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
  `ClassFile` varchar(255) NOT NULL,
  `Alive` tinyint(1) NOT NULL DEFAULT '1',
  `SuspectEquivalent` tinyint(1) NOT NULL DEFAULT '0',
  `DeclaredEquivalent` tinyint(1) NOT NULL DEFAULT '0',
  `Game_ID` int(11) DEFAULT NULL,
  `RoundCreated` int(11) NOT NULL,
  `RoundKilled` int(11) DEFAULT NULL,
  PRIMARY KEY (`Mutant_ID`),
  KEY `Game_ID` (`Game_ID`),
  CONSTRAINT `mutants_ibfk_1` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`Game_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
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
  `ClassFile` varchar(255) NOT NULL,
  `RoundCreated` int(11) NOT NULL,
  `MutantsKilled` int(11) DEFAULT '0',
  PRIMARY KEY (`Test_ID`),
  KEY `Game_ID` (`Game_ID`),
  CONSTRAINT `tests_ibfk_1` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`Game_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;
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
  `Password` varchar(20) NOT NULL,
  PRIMARY KEY (`User_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-11-04 12:25:43
