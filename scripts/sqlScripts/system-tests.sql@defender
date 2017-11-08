-- MySQL dump 10.13  Distrib 5.7.20, for Linux (x86_64)
--
-- Host: dbms.infosun.fim.uni-passau.de    Database: defender
-- ------------------------------------------------------
-- Server version	5.7.20-0ubuntu0.16.04.1

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

/*
 * This script reset the database to its initial state during system test. Basically it drops everything but classes and users.
 * At the moment is configured with the data from our setup @ Passau.
 */


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
  `AiPrepared` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`Class_ID`),
  UNIQUE KEY `classes_Alias_uindex` (`Alias`)
) ENGINE=InnoDB AUTO_INCREMENT=226 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` (`Class_ID`, `Name`, `JavaFile`, `ClassFile`, `Alias`, `AiPrepared`) VALUES
	(221,'Lift','/scratch/defender/system-tests/sources/Lift/Lift.java','/scratch/defender/system-tests/sources/Lift/Lift.class','Lift',0),
	(225,'Complex','/scratch/defender/system-tests/sources/Complex/Complex.java','/scratch/defender/system-tests/sources/Complex/Complex.class','Complex',0);
/*!40000 ALTER TABLE `classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `equivalences`
--

DROP TABLE IF EXISTS `equivalences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `equivalences` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `Mutant_ID` int(11) DEFAULT NULL,
  `Defender_ID` int(11) DEFAULT NULL,
  `Mutant_Points` int(11) DEFAULT '0',
  `Expired` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `ID_UNIQUE` (`ID`),
  KEY `fk_equiv_def_idx` (`Defender_ID`),
  KEY `fk_equiv_mutant_idx` (`Mutant_ID`),
  CONSTRAINT `fk_equiv_def` FOREIGN KEY (`Defender_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_equiv_mutant` FOREIGN KEY (`Mutant_ID`) REFERENCES `mutants` (`Mutant_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=182 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_chat`
--

DROP TABLE IF EXISTS `event_chat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `event_chat` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `Event_Id` int(11) DEFAULT NULL,
  `Message` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=71 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `event_messages`
--

DROP TABLE IF EXISTS `event_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `event_messages` (
  `Event_Type` varchar(45) NOT NULL,
  `Message` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`Event_Type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `events`
--

DROP TABLE IF EXISTS `events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `events` (
  `Event_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Game_ID` int(11) DEFAULT NULL,
  `Player_ID` int(11) DEFAULT NULL,
  `Event_Message` varchar(255) DEFAULT NULL,
  `Event_Type` varchar(45) DEFAULT NULL,
  `Event_Status` varchar(45) DEFAULT NULL,
  `Timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`Event_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=8330 DEFAULT CHARSET=utf8;
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
  `Start_Time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `Finish_Time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `Attackers_Limit` int(11) DEFAULT '0',
  `Defenders_Limit` int(11) DEFAULT '0',
  `State` enum('CREATED','ACTIVE','FINISHED','GRACE_ONE','GRACE_TWO') DEFAULT 'CREATED',
  `CurrentRound` tinyint(4) NOT NULL DEFAULT '1',
  `FinalRound` tinyint(4) NOT NULL DEFAULT '5',
  `ActiveRole` enum('ATTACKER','DEFENDER') NOT NULL DEFAULT 'ATTACKER',
  `Mode` enum('SINGLE','DUEL','PARTY','UTESTING') NOT NULL DEFAULT 'DUEL',
  `RequiresValidation` tinyint(1) NOT NULL DEFAULT '0',
  `IsAIDummyGame` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `fk_creatorId_idx` (`Creator_ID`),
  KEY `fk_className_idx` (`Class_ID`),
  CONSTRAINT `fk_classId` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_className` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_creatorId` FOREIGN KEY (`Creator_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=129 DEFAULT CHARSET=utf8;
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
  `MD5` char(32) NOT NULL,
  `ClassFile` varchar(255) DEFAULT NULL,
  `Alive` tinyint(1) NOT NULL DEFAULT '1',
  `Game_ID` int(11) NOT NULL,
  `RoundCreated` int(11) NOT NULL,
  `RoundKilled` int(11) DEFAULT NULL,
  `Equivalent` enum('ASSUMED_NO','PENDING_TEST','DECLARED_YES','ASSUMED_YES','PROVEN_NO') NOT NULL DEFAULT 'ASSUMED_NO',
  `Player_ID` int(11) DEFAULT NULL,
  `NumberAiKillingTests` int(11) DEFAULT '0',
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Points` int(11) DEFAULT '0',
  PRIMARY KEY (`Mutant_ID`),
  UNIQUE KEY `mutants_Game_ID_MD5_index` (`Game_ID`,`MD5`),
  KEY `fk_gameId_idx` (`Game_ID`),
  KEY `fk_playerId_idx` (`Player_ID`),
  CONSTRAINT `fk_gameId_muts` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_playerId_muts` FOREIGN KEY (`Player_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=3711 DEFAULT CHARSET=utf8;
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
  `Active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `players_User_ID_Game_ID_uindex` (`User_ID`,`Game_ID`),
  KEY `fk_userId_players_idx` (`User_ID`),
  KEY `fk_gameId_players_idx` (`Game_ID`),
  CONSTRAINT `fk_gameId_players` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_userId_players` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=514 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `registeredEmails`
--

DROP TABLE IF EXISTS `registeredEmails`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `registeredEmails` (
  `email` varchar(254) NOT NULL,
  PRIMARY KEY (`email`),
  UNIQUE KEY `validatedEmails_email_uindex` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

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
  KEY `fk_userId_sessions` (`User_ID`),
  CONSTRAINT `fk_userId_sessions` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=535 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=38660 DEFAULT CHARSET=utf8;
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
) ENGINE=InnoDB AUTO_INCREMENT=1882 DEFAULT CHARSET=utf8;
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
  `Email` varchar(254) NOT NULL,
  `Validated` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`User_ID`),
  UNIQUE KEY `users_email_index` (`Email`)
) ENGINE=InnoDB AUTO_INCREMENT=153 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`, `Validated`) VALUES (1,'Mutator','AI_ATTACKER_INACCESSIBLE','codedef_mutator@sheffield.ac.uk',0),(2,'TestGen','AI_DEFENDER_INACCESSIBLE','codedef_testgen@sheffield.ac.uk',0),(7,'bot','$2a$10$leVQAgRFxS4UihAq6h4NkuOOKtRNuf9TeUslsnB1ouo3ZDASsjc4e','bot@bot.bot',0),(8,'gordon','$2a$10$1GIeKUr2qYXt02OCJu32U.kUK/S6.G8TSIDF8KTJ8j168N6boGqie','gfraser79@gmail.com',0),(9,'stahnk02','$2a$10$hMHNzPCel7gaiVEiLZpqhuHcpnzl/UAAqxW4YBEY5RsdLEQ.3X4FS','stahnk02@gw.uni-passau.de',0),(10,'sarace01','$2a$10$.ZyKOGmyonquRD4v4EJycO7zWRsVT62YSpQqc16GiEY/ya288g4fe','sarace01@gw.uni-passau.de',0),(11,'goettl14','$2a$10$93Q77tga0eXi5xA8htrB.ewkTnArm5jWUcyiQdSUB.vwqrNkd9yJq','goettl14@gw.uni-passau.de',0),(12,'amin02','$2a$10$6gL.8u55tUJXIWtPeH75C.uabXSe8.3qNLVQM7GYsynU78f4LWEsO','amin02@gw.uni-passau.de',0),(13,'alemag01','$2a$10$0bovuODX0ybwoE9yL79xqee7lFjYq9oXDYaPVivjSvPFZ5ttOymf.','alemag01@gw.uni-passau.de',0),(14,'nechba01','$2a$10$rtoPSbm.awYU3AMT6GCIne3fU1Hzbq12KCAYOJjExfnuvVC87vwoi','nechba01@gw.uni-passau.de',0),(15,'settik01','$2a$10$kmPwVuIAb6El3smiWGVPWOfaBaLgVYUpKADdG3Z8/Ug7Sep7s5MJm','settik01@gw.uni-passau.de',0),(16,'bader14','$2a$10$5OhurdOXRNaFl8jGEw/nEeEC.xVJleGgPcgtkjO9WX7rL.4S.tN3S','bader14@gw.uni-passau.de',0),(17,'khatri02','$2a$10$ls83MOCOUJVcjJ8fARLeLuZD3YxD/k/l8KmOc7HC4ForFGaNzdsxa','khatri02@gw.uni-passau.de',0),(18,'mumbuv01','$2a$10$rcLVI6KchbNG8nHpzRrfYOt5/3Uaky/FxZLh8jlsqMRrx6l6BCPh6','mumbuv01@gw.uni-passau.de',0),(19,'govind04','$2a$10$J5Tfdkpj7ja9PVMPobZ.a.1m6SOZL72WraaCGbE5NrfgURVgBSZl6','govind04@gw.uni-passau.de',0),(20,'arfeen01','$2a$10$H8Td/YKxjpvKRv2ACJPj.OHnUio5NI3c8SN5ws.E0gpDaYvpgEUHu','arfeen01@gw.uni-passau.de',0),(21,'mhadhe01','$2a$10$BMUu7L/QLM3CU7Lj3b4iruOs2/9MYRAsTdeli04aF.GulgerkLS/W','mhadhe01@gw.uni-passau.de',0),(22,'ahmed05','$2a$10$l0WOVbVzBoELAGy17rRFI.DGlqENG58pE4ZZRp87wIQT2resHpZ3a','ahmed05@gw.uni-passau.de',0),(23,'lal01','$2a$10$q.B7y4CG.t3431ln6UzczO9LkFmNh4cnJtIbqf9Ec43xWLgO4B7N.','lal01@gw.uni-passau.de',0),(24,'joshi02','$2a$10$Zp4xsweYWKAA0ThsyD0Xm.6q3hvXNwMnMvZBcNHePQkjKjcJeobzO','joshi02@gw.uni-passau.de',0),(25,'zouaou01','$2a$10$VNFTOuATfV4Cixa72Ju.T.QKQc5qV51eh2Tv2MTUeDlajZ8F4q2Wu','zouaou01@gw.uni-passau.de',0),(26,'fuhr01','$2a$10$VO66TSqBHTENWUp3G.Ua9OS09hRTJm8ph15fzVP0.SW8okWyoUZeu','fuhr01@gw.uni-passau.de',0),(27,'jamil02','$2a$10$s/HhlhmY0kpLVFiu2uQO0OA.rw8.p3ZHZ75AcvQeBP2vS5idoQwdy','jamil02@gw.uni-passau.de',0),(28,'hirpar01','$2a$10$ODxB.pCxSAiCEKlUtIAADOxsfBw6yoy.IPFBXgv0uNN.jwO4PcXXa','hirpar01@gw.uni-passau.de',0),(29,'pradha02','$2a$10$KWhGdtL64p8nEmIL0w6gu.55TTrSVdPf9gHIFbQ9jtPERNlDq.Cji','pradha02@gw.uni-passau.de',0),(30,'sokyap01','$2a$10$ntltuW21sD0rup85GcUCU.YHO8lJakp2cVx1Smvbs.oztUn2PsOnO','sokyap01@gw.uni-passau.de',0),(31,'franci01','$2a$10$jfxpWlZB6zuHQG4b.2krXOJRl.6066DiikqKr9hZ5hzWn7wtqtz/6','franci01@gw.uni-passau.de',0),(32,'zarrou01','$2a$10$MM5jCfiJhohFofxeeJDR0ejKKLub2YcB3fAFI.dNdRPJOuq5QyypC','zarrou01@gw.uni-passau.de',0),(33,'forhad01','$2a$10$5P2OFacC5X8C0K4980OM9.qgYtBaXm0/v2pwELT.W.J01tPhea7sO','forhad01@gw.uni-passau.de',0),(34,'halgek01','$2a$10$CZIcTDXzWBjwpi0seaFn7.Bg4Pt9s760Fty6AR7UfgFb7oGl6TOPe','halgek01@gw.uni-passau.de',0),(35,'deepak01','$2a$10$Gj2Avz74KY8.Weyrp9kDSeS5NIfhSIO8HSphKCs7z0JR5mKdlShDW','deepak01@gw.uni-passau.de',0),(36,'ashuto01','$2a$10$9VjX/EQm368qrb7DNv2GfeilLzr.Qtv8jP8IT9mtK9X8x7g3n1Wuy','ashuto01@gw.uni-passau.de',0),(37,'maring02','$2a$10$50y/ksC7npD/.oPb7UaD0uo6W.EjKVk7YO9L.63WUqAJgmmt2.1lK','maring02@gw.uni-passau.de',0),(38,'kadam01','$2a$10$PraiQBVM06T.4OoyBB4tU.Pga8lYfzAPllIyHe9zFhwh12bG3ichy','kadam01@gw.uni-passau.de',0),(39,'pauluk01','$2a$10$xBvPEvcPu2Mz.81eZNFY9eiz/TXRPfoE2Qvj1q2VG4avb1kZMZaU.','pauluk01@gw.uni-passau.de',0),(40,'vijaya02','$2a$10$pCPL6MGH2zPyjqV5ZXNmJOX6jNQLZ2MzU/6gFUkFHLd5qxsSEiN5e','vijaya02@gw.uni-passau.de',0),(41,'reich11','$2a$10$fiQ0VCHdL8pQHsrpsJ8wxO2CEt6FXXg5gJLIBbbgZaeJTnTVlMTVe','reich11@gw.uni-passau.de',0),(42,'bhuiya02','$2a$10$UAK3JKsCXXip4Hq1f10Q7uhdxifpvGTpvWuiAmmyH0AJpMlPKBs/e','bhuiya02@gw.uni-passau.de',0),(43,'lukasc01','$2a$10$sod3OsXOdEc7OH7cuJQDH.dVQMu3XQmEc5D4OTvreHCfxdi/eJzHq','lukasc01@gw.uni-passau.de',0),(44,'gojaye01','$2a$10$YFzLbcBFz0iIROlzJcquKuZLEAzxUCsz0CUI6pyBPIhJehFm54WUi','gojaye01@gw.uni-passau.de',0),(45,'verma01','$2a$10$5m3edXrj0Qb2gqqeUl7Joeuj0rqYu2572o7mtuzbeCxMm1wd4WoZK','verma01@gw.uni-passau.de',0),(46,'udaysh01','$2a$10$zeJ8plDnpI2Z9RncWBKfCekiU4X5ejC2.lAG5r.J9BZO3bncOJAFm','udaysh01@gw.uni-passau.de',0),(47,'patil04','$2a$10$Ld/i9YIWf4EvM8d5mrKotez1wCFJNa03ndU4PDDhROdOljR9T7obO','patil04@gw.uni-passau.de',0),(48,'bouzay01','$2a$10$YP.T8p93TT.JYKioq1IVzOmiqFh8p92wpXfeU2uHJJSazseSEYFs6','bouzay01@gw.uni-passau.de',0),(49,'jothip01','$2a$10$wYHYD0r7oclldW.QRUKLNulXsc1.nLeSc0e7CB850Ije743Lnjqw6','jothip01@gw.uni-passau.de',0),(50,'somai01','$2a$10$JzImiA2.gIlBIrPcY2W.rO5T6w1bZkelHfjeGE7Ui6qx1Q2rqtQh.','somai01@gw.uni-passau.de',0),(51,'panach01','$2a$10$KdyBN6sYQff3j9WZMoQS1utEuSCY5GnJ.3OQPUttHPOjCd172aJ/y','panach01@gw.uni-passau.de',0),(52,'khan05','$2a$10$fF4gMAx.DDyZAo2g.1RBVuC23Ln3.7sMhO0ppDwMwOBBK..Vsc4Ge','khan05@gw.uni-passau.de',0),(53,'sukuma02','$2a$10$kops0KlFvflAg/lPRa0PpOakvlAN7wkF331LgelPPFIpzQTNrRrTi','sukuma02@gw.uni-passau.de',0),(54,'koroma01','$2a$10$wf1kU3z8LO6KXY3UMMizLe/Bf0bqv6r1P.2WAWn.e3ACRn1S76Zo2','koroma01@gw.uni-passau.de',0),(55,'mannar01','$2a$10$dUKScbdGR/AEVPHStmpo1erEPP41sap85YPvLWtU3CXxRO.RhnxZS','mannar01@gw.uni-passau.de',0),(56,'wendli01','$2a$10$Y8HlIklnPRok9SgAI5d2beOa9FcpuTC40sR4gvcOW8yE8uB8LtxNG','wendli01@gw.uni-passau.de',0),(57,'li25','$2a$10$B0ie1O700zk5ZlKdhJ4G0ujScr01vo8YlKnek2GyU4HAPVrXGZEa6','li25@gw.uni-passau.de',0),(58,'gidwan01','$2a$10$Ucf1KPCRb/BrWVvWAYC3Ieob1ypNhNXfYthT5p.JU7QDJSvfq6c9W','gidwan01@gw.uni-passau.de',0),(59,'benais01','$2a$10$h0t7SZXO8MMewwlKMTGbRuhsf2fw89wyNVG2tkKS7n1QqdXbrj4eC','benais01@gw.uni-passau.de',0),(60,'tariq01','$2a$10$7kGX3fJYMk8Lo7Jr3h0b7uYiCMa0MWlS.Xif8t9K96f01ehDUhDLC','tariq01@gw.uni-passau.de',0),(61,'singh06','$2a$10$.SBrrvdpI5x7EqX7tGA5wuRblUa89GfRjuhc6MDnbGwnT6r5wEs5y','singh06@gw.uni-passau.de',0),(62,'doerin14','$2a$10$OTtZ3Sn0LTJzktUhQDDZcOVX5N1WazQnmK/As1PaDSAdANesUkedq','doerin14@gw.uni-passau.de',0),(63,'mazhar01','$2a$10$KPIAJeLsDKcelxMcs4p2Y.gq8viFkn92ZubBqELoa8OoOBkwU2Eii','mazhar01@gw.uni-passau.de',0),(64,'werny01','$2a$10$7NwV6rVTHA6xvcD61PqBr.Tl6aeHlHnQ0k1..I9.3i0ROCDtJvCEK','werny01@gw.uni-passau.de',0),(65,'lalitk01','$2a$10$7YWbO8LzaaPp0rvNJbHR4Ok2eNWs8GQjnqtbe3pLZ5KiWtWKFRuhu','lalitk01@gw.uni-passau.de',0),(66,'kadavi01','$2a$10$/6GQWnL9Xfl.drXg7.kEs.Ye6qOC7kzYvJP0x263/mpI/ulPnbI2y','kadavi01@gw.uni-passau.de',0),(67,'straub24','$2a$10$1E0arsf00aHQLgqlbQQ/MeHOKiMY4.Y5H.8JXqoiySnVUN3I.wzvm','straub24@gw.uni-passau.de',0),(68,'bauer167','$2a$10$OE3EkPAK28L.T01yyIKVg.VTxleS3azMfwGQOyvmFOdx1GCjaWfK6','bauer167@gw.uni-passau.de',0),(69,'kohlgr01','$2a$10$SvhZoPy3c5GsaRycjbn2H.ZNVQ7SugL6P5qht15tel4CyuCh/GzUm','kohlgr01@gw.uni-passau.de',0),(70,'alabdu01','$2a$10$NKs0tQXHfzGptgPPAEDq5uPfD.ywavpdPOheScVEoomgglNT7CRvK','alabdu01@gw.uni-passau.de',0),(71,'ramesh01','$2a$10$FoPBDMJDWiP5VERj6quMeeVJqvCNBOrb5HO0whcFUgceB6RKAbTzS','ramesh01@gw.uni-passau.de',0),(72,'boukha01','$2a$10$53EjoL30CpbMHQrP.ZrIzuSPPMxirf9twj8qp90FcE/Bf3DIQjJMK','boukha01@gw.uni-passau.de',0),(73,'christ38','$2a$10$bCGri2uixYigvkGviRD81uO3F25vs43whVr679NktFh82S1X9pd4u','christ38@gw.uni-passau.de',0),(74,'brunne83','$2a$10$V41AVnEeohn.3z.g8SQVhen0nKUuddvnGbq84PGFMGT53YrGEOEf2','brunne83@gw.uni-passau.de',0),(75,'stegne01','$2a$10$oGmSfjFUFoMGBBQmTBzpQe33Z8h1OOmVQxwK4jhaYuTLwr5TCyevK','stegne01@gw.uni-passau.de',0),(76,'chatta01','$2a$10$q0ZNJu4BjiNrGdTBI4r7LuMhCTuGwa84nuoIqf30u1BJHW15AoLJ2','chatta01@gw.uni-passau.de',0),(77,'perika01','$2a$10$jRt1G8D2sk8qge9e/QxXDODd4To.W/5.bIdnAY2JlJK7U9YqWSikK','perika01@gw.uni-passau.de',0),(78,'mellou01','$2a$10$YkDROvu3uqCZHMVZ6s9CYe7DokpUbD9QcnprptoVWwE7qMe9vyssG','mellou01@gw.uni-passau.de',0),(79,'bhuiya01','$2a$10$VHd33mUoGZJlyJ/fAK2J4O9P4ZqvsxDev5d3ZqfRLazXUcJafwQH.','bhuiya01@gw.uni-passau.de',0),(80,'boehm45','$2a$10$1zSvVmY5OgDzJboOVcG93.11OIuRjMqw1bAHNYnCZbPq5zsFPaTEC','boehm45@gw.uni-passau.de',0),(81,'durair01','$2a$10$/jnXkgd6AhZQyYc20hnjOeqs.hHCRnMXvJlbU80MrxbLSTXnEKzDq','durair01@gw.uni-passau.de',0),(82,'gupta03','$2a$10$NT7AwdzYiZFAoRz2TB.JF.AG5awQSG/NwDTl.0C1aNZjVgcv2iUdO','gupta03@gw.uni-passau.de',0),(83,'ali07','$2a$10$M8VjKV4vzv0ntWRKHLrDhO6270j/kxC7I/YDaP6BNiy0tBy4T9BFO','ali07@gw.uni-passau.de',0),(84,'jon01','$2a$10$/2WWcJIHE8N7An7Q3SRA6uawFlGLhVMbO2/HWI37HRr3fUZCgyHMi','jon01@gw.uni-passau.de',0),(85,'govind03','$2a$10$h192lTtDKGOeggp7SWje7eXddEUpEpHryeJg6T5o11.jNkqAPQvqe','govind03@gw.uni-passau.de',0),(86,'chikha02','$2a$10$Y3ibiWqQkYk.O9Dx8GpYH.3Qmnq7RR8BQlHgLOTNQfimMss.8AWWy','chikha02@gw.uni-passau.de',0),(87,'breite12','$2a$10$ldSc8rYW4N2MTyDebDD6N.sH2AllOd77eucOMKvLEaWVUvguMTu9y','breite12@gw.uni-passau.de',0),(88,'javed01','$2a$10$vP2JjKSM/xL.Z54O/rJKkOTBRwkI/DhQaqiEQuvKXZiXWf3dmgn2q','javed01@gw.uni-passau.de',0),(89,'goetz26','$2a$10$clDld/ZHhAGwZEcS8ub6EufMHqk5dkGg9fn8uqtc58FHamOcXXrZy','goetz26@gw.uni-passau.de',0),(90,'griebl06','$2a$10$Vjw56JgOsAnYXdsqrwSLbeYqF9aS67NUT/AmljADtu1M7LKE2vVMS','griebl06@gw.uni-passau.de',0),(91,'ahsan01','$2a$10$FedRWIjECf8ojDoygvfTaeFdIhHofr71MnJjMHSn2bdo0erGv1cJC','ahsan01@gw.uni-passau.de',0),(92,'singh05','$2a$10$ZjtruAkHvCDexNcTVy2e6OlMSiDaBUxeXj6BwPK7GHUF5NTeFjWC.','singh05@gw.uni-passau.de',0),(93,'hegman02','$2a$10$eTmVkyOS1IeVrzhCsxAmlOFyC8X9XKDAQ94PZqcGQfE4Co2z8mLgq','hegman02@gw.uni-passau.de',0),(94,'ebel02','$2a$10$/Eo5gzsQpgBLdRptwCSa9ODzJ5QK/O2L11a5u1v1vNXYRqTpu3nUK','ebel02@gw.uni-passau.de',0),(95,'nazir01','$2a$10$rGceBYusC96PP7ThQ2v08OkRrn4nNpptgMcE/SL.WzvhnhURyEefS','nazir01@gw.uni-passau.de',0),(96,'birken06','$2a$10$ldtSlk09weGn.7FfMH2L5OYI01nX8WccH6yD9T76PrRSS1s7JtQ3C','birken06@gw.uni-passau.de',0),(97,'jain02','$2a$10$r71eCluqE.XyqVU1bk4fkekDj466j.es4jr/Cga5YgL.ZUFZw//VG','jain02@gw.uni-passau.de',0),(98,'benamo01','$2a$10$mc68mp/6nuamY.iBx0BVTewqtcFXOqrBSOQZBTWkfW8DK0tv/i1P.','benamo01@gw.uni-passau.de',0),(99,'semeno02','$2a$10$cdqzwA44g09sVwz9itaNCOwt/bM9o52gz.rz3ZBna4L6wL.7VsPqO','semeno02@gw.uni-passau.de',0),(100,'kidar01','$2a$10$Os5LwDkoZ9M4ZffWvggxQek4BKtwYyuTd2fvXkXMfMmMcd./34BBS','kidar01@gw.uni-passau.de',0),(101,'gogine01','$2a$10$3lCIsOwPR4xpPhbGJE2pYuswIPE4P3nsWUuNDRBAmWVBdM66yOq16','gogine01@gw.uni-passau.de',0),(102,'holze01','$2a$10$WGwSlQhh.Mgu4mU1UKI6leFYlrJZj3lyKX.crUoflCSJT4i7XUxf6','holze01@gw.uni-passau.de',0),(103,'manjun01','$2a$10$9ic76cqD37AY0z0ijNtaievHg0rX1jL3rCo.6wVDdETwyFiJyZ8fu','manjun01@gw.uni-passau.de',0),(104,'dubba01','$2a$10$wsVMLGFKJiYCX03I8TsRqO9B3qV3S2fOb.aFA7RzDyvgENIGkeuIK','dubba01@gw.uni-passau.de',0),(105,'lahbai01','$2a$10$m6k7vnf.EHJXhnuBgeICL.qTiH.dDM5ShqR/MQtVQpTmydiOfL22.','lahbai01@gw.uni-passau.de',0),(106,'avilah01','$2a$10$GWuOCgI3JZAne/e20M3Yvu6Qg2NNXNsOQl5XNvYO6i/vq2DFQ8jSm','avilah01@gw.uni-passau.de',0),(107,'galdob01','$2a$10$gxYB3Knc5s7zmkGth9L6AufsbJ.hAG/vlFncuTGbMLcfPXKPdrYqy','galdob01@gw.uni-passau.de',0),(108,'sayhi01','$2a$10$6UcoDdamjBvJziATRvrH1e4EoKOM8sxpLj8UF4K/CPwfxVqJpEgD6','sayhi01@gw.uni-passau.de',0),(109,'gulati01','$2a$10$OvF9EQ6gg7phaDitxm2mROnvyZWa9zcUp9GS9DUm.nQHHnQ7sJR3S','gulati01@gw.uni-passau.de',0),(110,'schwei60','$2a$10$xd5jWtsTDtqrPqB1AH/tZ.aWuAfXNLSfDhmwR9oUFivkF48eRW2ym','schwei60@gw.uni-passau.de',0),(111,'osterh06','$2a$10$RPfprzi9B2gzqaxxIunEweVXHI5kkBzrcxvOuapjvX8quaIi4a.zi','osterh06@gw.uni-passau.de',0),(112,'lamiri01','$2a$10$UYBWDWWBurwinY3EV77dWuzwPCQ003uBkqSv/zTvLpCfxVaZiI2Xa','lamiri01@gw.uni-passau.de',0),(113,'chahed01','$2a$10$4YoiBgaCFlqoPjqZ7O674OAR7zY4UPybAJxVatwRhSHl5pHeGS3Em','chahed01@gw.uni-passau.de',0),(114,'bah01','$2a$10$/Q8Mw.m.LdNJYS9B2WMJX.E9hwRI09E0ZM0cB4b.IZACYVdZBD/Sq','bah01@gw.uni-passau.de',0),(115,'niemei02','$2a$10$.h6fJC3I3VRnNUgX/3UxH.5r6Xxlyjs4ohNIBOoHQpcNCPPq7u69q','niemei02@gw.uni-passau.de',0),(116,'oeztue04','$2a$10$RjxeKmpaFv.dVJM38mfFpOIxitTOfVfRjdk8xqTisGtRiiRQFGHEO','oeztue04@gw.uni-passau.de',0),(117,'auer33','$2a$10$Y.Bd/ViFNvy6pbKZf2ofT.u14ebcB/4gsSVEkPmci4gb9v6p73wUO','auer33@gw.uni-passau.de',0),(118,'chidam01','$2a$10$UGUTFMTB6psWqDQlmnU7zOPHDSWv7RZztC/ugVfeHHgPzCSRtJqnG','chidam01@gw.uni-passau.de',0),(119,'prabha01','$2a$10$ALkOOUWpf7FZeITjme7ZHO1yJyqbW.P9TdwAOK1g2hPtqepC.Dx8K','prabha01@gw.uni-passau.de',0),(120,'lokana01','$2a$10$f5S8ZAf49dYuaFuXXLK2G.UxYZTwMW15iGS1hoaWMuFrbbBPQ4Bsi','lokana01@gw.uni-passau.de',0),(121,'muell326','$2a$10$gioMrDCEUBHg7omqsbYwUOVCti.ee6bUNVmGFfvbtKrj82J8Qugse','muell326@gw.uni-passau.de',0),(122,'gruber59','$2a$10$7Rg..1cZyzMEIzJAoXZ/YO1l6bPAC57TswyyLGznOJkqWDcpz4qd.','gruber59@gw.uni-passau.de',0),(123,'prasse02','$2a$10$db2.yIdm728lj.XCxO2K8O2EGr1OQjYn/6J413z.Akx0fP3vX0uYa','prasse02@gw.uni-passau.de',0),(124,'singh08','$2a$10$7GwocVjRYNN5UyzT0a7YVemDpFa0/GWHY/sj2NsSi4RapvlSrK7U6','singh08@gw.uni-passau.de',0),(125,'iqbal01','$2a$10$Ii71TLImzCaTD2mPk1osOOqxrJAyaE2M5RjhMW9Mxvl5WzPtsWid6','iqbal01@gw.uni-passau.de',0),(126,'muenic07','$2a$10$g7Tjt71sTAPemm.GiAvk2OE/jzZFmAwwfFFzLWW43Pl3CCzriFsa6','muenic07@gw.uni-passau.de',0),(127,'ayoub01','$2a$10$Fm8/YboSyTZUZeJVM/9JcOwAxHTJxzwGTsELJ/yltYOpcOWO9rrty','ayoub01@gw.uni-passau.de',0),(128,'ahmadi05','$2a$10$2i9vQgYJktm3Zpwn4It5tepb3IyE4A1h.0U73xVrVMNn4TGpJYTFW','ahmadi05@gw.uni-passau.de',0),(129,'demoattacker','$2a$10$eMu9i79QEFFkXClufLhS..kLJlExyx21kRprwQ22TeVUIKjuQVHni','gfraser79+attacker@gmail.com',0),(130,'demodefender','$2a$10$0ruQ.pYPr7q1gzpXWZ372OIYvL/KrLLxA.FsbGSQStf6F/JV00Q9i','gfraser79+defender@gmail.com',0),(131,'ayaz01','$2a$10$Y8a1DaUuVYxUUFNPyPO/zu62vbPLCBRuXhrkD71mcbCWgAS6/EbfG','ayaz01@gw.uni-passau.de',0),(132,'khan06','$2a$10$3MOiSFYGrNEGVY/BQan5eOQwCyDFMAemnvU86.WKnfNLE6LQI422i','khan06@gw.uni-passau.de',0),(133,'ehtesh01','$2a$10$j2L.BIZOWzfQSrG1EkMN0uI0fDgUoaZkyP1JSoF84q8cRZR0mmBM.','ehtesh01@gw.uni-passau.de',0),(134,'hussei03','$2a$10$i56uvfJgMq3jOSVjuICeX.QlcuXhh38LyRNOzPeOekNlX7PV.AiXq','hussei03@gw.uni-passau.de',0),(135,'rezaei01','$2a$10$HJb7RNuA0Bpk1n.tLxtOvOrOHjtDjFflTYoOrfZLVgfUEOKC1vYMK','rezaei01@gw.uni-passau.de',0),(136,'sameedtariq','$2a$10$cHwniU.OMBXf9ch.7AcctOLQlmGZAU7GrjSUnT0cV0ae67IrVAwQC','tariq01@uni-passau.de',0),(137,'hossai01','$2a$10$yinxBPhoTQRApEDlrexzX.YAqylDMZbE.4gG.07jX5atzUrsNnEIm','hossai01@gw.uni-passau.de',0),(138,'khanna02','$2a$10$ZCEHof.q6nSpMxW/.Oe85eIL33PrzQqSELiL761g52VxHacBIhIbu','khanna02@gw.uni-passau.de',0),(139,'tkachu01','$2a$10$bJIWH7nX3DWTgmNZUqY9Le6xRkhLf.1m6sD8hz3QDjtSY.bfH8v3m','tkachu01@gw.uni-passau.de',0),(140,'holosy01','$2a$10$0vW8hGbZ8b/e5G0es5LDEOt8.6sVZxsbt0vMH89XKi1ytEuGmN.aa','holosy01@gw.uni-passau.de',0),(141,'sell03','$2a$10$2S.i2.pr.KkIOV/NQT7xO.68uM2uXbebOSFNuIhARB/goIK3BBGX6','sell03@gw.uni-passau.de',0),(142,'gruber55','$2a$10$POyEeyzHe2wppkcwn3rX6eKwLnmSZ7UtumXzhYZkcA3CLmSdveBiq','gruber55@gw.uni-passau.de',0),(143,'werli02','$2a$10$3Cv28ZYkasOulZNp7sw7EOBR/4GPR37R5c.vSIhyaXTYno7E2ErFm','werli02@gw.uni-passau.de',0),(144,'mtir01','$2a$10$1MwzaHEmf3diomAFCJfkhecd8cgCyjvM6b5IsJoTiJqnhlGV5r2y6','mtir01@gw.uni-passau.de',0),(145,'murali01','$2a$10$A3MWLV.I9/6Hv3UpDep1D.6ZXbi1j1cVDXV44ub3rBe.bikFo5Ud2','murali01@gw.uni-passau.de',0),(146,'benoth01','$2a$10$x.3eEJ/nQ0EtXWV4wvQ92eNchqhCS5MNlZRJlrKVtMIGIfX5BX4zC','benoth01@gw.uni-passau.de',0),(147,'majed01','$2a$10$Z0f5wJMLLl0DqegYsnmvxeFqrFn3nt7i5EAspr8mtYTtA.Xo6MoVO','majed01@gw.uni-passau.de',0),(148,'ghosh01','$2a$10$qtpfQO0cmHY0YrOsoqdAqOtW6p.4XXP7BcDXwwEMoHsuGloa3jEaa','ghosh01@gw.uni-passau.de',0),(149,'bostan01','$2a$10$SC9R5a4CKD77Yoj0vzBMSOKMPU1UEVBtEkS8v577s.vkZu5Yx5RjG','bostan01@gw.uni-passau.de',0),(150,'siddiq01','$2a$10$7BRFIoNn6s.J94sJat8rT.cTrFPPE1mQDGzswOekVzW2ljfWY8zEi','siddiq01@gw.uni-passau.de',0),(151,'choura01','$2a$10$3IclOQfefQJW2yXXoljLf.yxU7OQN/L4TtCZLC.AyuxwUeW1Ex592','choura01@gw.uni-passau.de',0),(152,'test','$2a$10$xr6eGsAYVV4AtuYGt/qM/ej3ChdfFn2AW90dWcPBTCZAMEJ2h296S','test@gw.uni-passau.de',0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`defender`@`%`*/ /*!50003 TRIGGER ins_users
BEFORE INSERT ON `users`
FOR EACH ROW BEGIN
  IF (NEW.Email IN (SELECT * FROM registeredEmails)) THEN
    SET NEW.Validated = TRUE;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Temporary table structure for view `view_leaderboard`
--

DROP TABLE IF EXISTS `view_leaderboard`;
/*!50001 DROP VIEW IF EXISTS `view_leaderboard`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `view_leaderboard` AS SELECT 
 1 AS `username`,
 1 AS `NMutants`,
 1 AS `AScore`,
 1 AS `NTests`,
 1 AS `DScore`,
 1 AS `NKilled`,
 1 AS `TotalScore`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `view_leaderboard`
--

/*!50001 DROP VIEW IF EXISTS `view_leaderboard`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`defender`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `view_leaderboard` AS select `U`.`Username` AS `username`,ifnull(`Attacker`.`NMutants`,0) AS `NMutants`,ifnull(`Attacker`.`AScore`,0) AS `AScore`,ifnull(`Defender`.`NTests`,0) AS `NTests`,ifnull(`Defender`.`DScore`,0) AS `DScore`,ifnull(`Defender`.`NKilled`,0) AS `NKilled`,(ifnull(`Attacker`.`AScore`,0) + ifnull(`Defender`.`DScore`,0)) AS `TotalScore` from ((`defender`.`users` `U` left join (select `PA`.`User_ID` AS `user_id`,count(`M`.`Mutant_ID`) AS `NMutants`,sum(`M`.`Points`) AS `AScore` from (`defender`.`players` `PA` left join `defender`.`mutants` `M` on((`PA`.`ID` = `M`.`Player_ID`))) group by `PA`.`User_ID`) `Attacker` on((`U`.`User_ID` = `Attacker`.`user_id`))) left join (select `PD`.`User_ID` AS `user_id`,count(`T`.`Test_ID`) AS `NTests`,sum(`T`.`Points`) AS `DScore`,sum(`T`.`MutantsKilled`) AS `NKilled` from (`defender`.`players` `PD` left join `defender`.`tests` `T` on((`PD`.`ID` = `T`.`Player_ID`))) group by `PD`.`User_ID`) `Defender` on((`U`.`User_ID` = `Defender`.`user_id`))) where (`U`.`User_ID` > 2) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-11-03 11:17:08
