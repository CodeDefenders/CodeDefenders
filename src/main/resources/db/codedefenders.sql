-- MySQL dump 10.13  Distrib 5.7.9, for Win64 (x86_64)
--
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

DROP TABLE IF EXISTS `settings`;
CREATE TABLE settings
(
  name         VARCHAR(50) PRIMARY KEY NOT NULL,
  type         ENUM ('STRING_VALUE', 'INT_VALUE', 'BOOL_VALUE'),
  STRING_VALUE TEXT,
  INT_VALUE    INTEGER,
  BOOL_VALUE   BOOL
);

INSERT INTO settings (name, type, STRING_VALUE, INT_VALUE, BOOL_VALUE) VALUES
  ('SHOW_PLAYER_FEEDBACK', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('REGISTRATION', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('CLASS_UPLOAD', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('GAME_CREATION', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('REQUIRE_MAIL_VALIDATION', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('SITE_NOTICE', 'STRING_VALUE', '', NULL, NULL),
  ('PASSWORD_RESET_SECRET_LIFESPAN', 'INT_VALUE', NULL, 12, NULL),
  ('MIN_PASSWORD_LENGTH', 'INT_VALUE', NULL, 8, NULL),
  ('CONNECTION_POOL_CONNECTIONS', 'INT_VALUE', NULL, 20, NULL),
  ('CONNECTION_WAITING_TIME', 'INT_VALUE', NULL, 5000, NULL),
  ('EMAIL_SMTP_HOST', 'STRING_VALUE', '', NULL, NULL),
  ('EMAIL_SMTP_PORT', 'INT_VALUE', '', NULL, NULL),
  ('EMAIL_ADDRESS', 'STRING_VALUE', '', NULL, NULL),
  ('EMAILS_ENABLED', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('DEBUG_MODE', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('EMAIL_PASSWORD', 'STRING_VALUE', '', NULL, NULL);

--
-- Table structure for table `ratings`
--

DROP TABLE IF EXISTS `ratings`;
CREATE TABLE ratings
(
  User_ID INT DEFAULT -1 NOT NULL,
  Game_ID INT DEFAULT -1 NOT NULL,
  type    VARCHAR (128),
  value INT DEFAULT 0 NOT NULL,
  CONSTRAINT fk_ratings_userID_users FOREIGN KEY (User_ID) REFERENCES users (User_ID)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_ratings_gameID_games FOREIGN KEY (Game_ID) REFERENCES games (ID)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT game_user_type_unique UNIQUE (User_ID, Game_ID, type)
);

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
  `AiPrepared` TINYINT(1) DEFAULT '0',
  `RequireMocking` TINYINT(1) DEFAULT '0',
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
  `Level` enum('EASY','MEDIUM','HARD') DEFAULT 'HARD',
  `Timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `Creator_ID` int(11) DEFAULT NULL,
  `Prize` int(11) DEFAULT NULL,
  `Defender_Value` int(11) DEFAULT '100',
  `Attacker_Value` int(11) DEFAULT '100',
  `Coverage_Goal` float DEFAULT NULL,
  `Mutant_Goal` float DEFAULT NULL,
  `Attackers_Needed` int(11) DEFAULT '0',
  `Defenders_Needed` int(11) DEFAULT '0',
  `Start_Time` TIMESTAMP DEFAULT '1970-02-02 01:01:01',
  `Finish_Time` TIMESTAMP DEFAULT '1970-02-02 01:01:01',
  MaxAssertionsPerTest INT DEFAULT 2 NOT NULL,
  MutantValidator ENUM('STRICT', 'MODERATE', 'RELAXED') DEFAULT 'MODERATE' NOT NULL,
  MarkUncovered BOOL DEFAULT FALSE  NOT NULL,
  ChatEnabled BOOL DEFAULT TRUE  NULL,
  `Attackers_Limit` int(11) DEFAULT '0',
  `Defenders_Limit` int(11) DEFAULT '0',
  `State` enum('CREATED','ACTIVE','FINISHED','GRACE_ONE','GRACE_TWO') DEFAULT 'CREATED',
  `CurrentRound` tinyint(4) NOT NULL DEFAULT '1',
  `FinalRound` tinyint(4) NOT NULL DEFAULT '5',
  `ActiveRole` enum('ATTACKER','DEFENDER') NOT NULL DEFAULT 'ATTACKER',
  `Mode` enum('SINGLE','DUEL','PARTY','UTESTING') NOT NULL DEFAULT 'DUEL',
  `RequiresValidation` TINYINT(1) DEFAULT '0' NOT NULL,
  `IsAIDummyGame` TINYINT(1) DEFAULT '0' NOT NULL,
  `HasKillMap` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `fk_creatorId_idx` (`Creator_ID`),
  KEY `fk_className_idx` (`Class_ID`),
  CONSTRAINT `fk_classId` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_className` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_creatorId` FOREIGN KEY (`Creator_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `hasKillMap` CHECK (HasKillMap = 0 OR State = 'FINISHED') -- only finished games can have a killmap
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `killmap`
--

DROP TABLE IF EXISTS `killmap`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `killmap` (
  `Class_ID` int(11) NOT NULL,
  `Game_ID` int(11) DEFAULT NULL,
  `Test_ID` int(11) NOT NULL,
  `Mutant_ID` int(11) NOT NULL,
  `Status` enum('KILL','NO_KILL','NO_COVERAGE','ERROR','UNKNOWN') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`Test_ID`,`Mutant_ID`),
  KEY `fk_killmap_classId` (`Class_ID`),
  KEY `fk_killmap_gameId` (`Game_ID`),
  KEY `fk_killmap_mutantId` (`Mutant_ID`),
  CONSTRAINT `fk_killmap_classId` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_killmap_gameId` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_killmap_mutantId` FOREIGN KEY (`Mutant_ID`) REFERENCES `mutants` (`Mutant_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_killmap_testId` FOREIGN KEY (`Test_ID`) REFERENCES `tests` (`Test_ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `MD5` CHAR(32) NOT NULL,
  `ClassFile` varchar(255) DEFAULT NULL,
  `Alive` tinyint(1) NOT NULL DEFAULT '1',
  `Game_ID` int(11) NOT NULL,
  `RoundCreated` int(11) NOT NULL,
  `RoundKilled` int(11) DEFAULT NULL,
  `Equivalent` enum('ASSUMED_NO','PENDING_TEST','DECLARED_YES','ASSUMED_YES','PROVEN_NO') DEFAULT 'ASSUMED_NO' NOT NULL,
  `Player_ID` int(11) DEFAULT NULL,
  `NumberAiKillingTests` int(11) DEFAULT '0', /* If an original ai mutant, killcount. Number of killing tests in game otherwise. */
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Points` int(11) DEFAULT '0',
  PRIMARY KEY (`Mutant_ID`),
  KEY `fk_gameId_idx` (`Game_ID`),
  KEY `fk_playerId_idx` (`Player_ID`),
  CONSTRAINT `fk_gameId_muts` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_playerId_muts` FOREIGN KEY (`Player_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=125 DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX mutants_Game_ID_MD5_index ON mutants (Game_ID, MD5);
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
  `Active` TINYINT(1) DEFAULT '1' NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `fk_userId_players_idx` (`User_ID`),
  KEY `fk_gameId_players_idx` (`Game_ID`),
  CONSTRAINT `fk_gameId_players` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_userId_players` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX players_User_ID_Game_ID_uindex ON players (User_ID, Game_ID);
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
  `NumberAiMutantsKilled` int(11) DEFAULT '0', /* If an original ai test, killcount. Number of kills in game otherwise. */
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
-- Table structure for table `registeredEmails`
--
DROP TABLE IF EXISTS `registeredEmails`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `registeredEmails` (
  email VARCHAR(254) PRIMARY KEY NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX validatedEmails_email_uindex ON registeredEmails (email);
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

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
  `Validated` TINYINT(1) DEFAULT '0' NOT NULL,
  `Active` TINYINT(1) DEFAULT '1' NOT NULL,
  pw_reset_timestamp TIMESTAMP DEFAULT NULL  NULL,
  pw_reset_secret VARCHAR(254) DEFAULT NULL  NULL,
  PRIMARY KEY (`User_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX users_email_index ON users (Email);
CREATE UNIQUE INDEX users_pw_reset_secret_uindex ON users (pw_reset_secret);
DELIMITER $$
CREATE TRIGGER ins_users
BEFORE INSERT ON `users`
FOR EACH ROW BEGIN
  IF (NEW.Email IN (SELECT * FROM registeredEmails)) THEN
    SET NEW.Validated = TRUE;
  END IF;
END$$
DELIMITER ;
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
) ENGINE=InnoDB AUTO_INCREMENT=117 DEFAULT CHARSET=utf8;

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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
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
-- Dumping data for table `event_messages`
--

LOCK TABLES `event_messages` WRITE;
/*!40000 ALTER TABLE `event_messages` DISABLE KEYS */;
INSERT INTO `event_messages`
VALUES
  ('ATTACKER_JOINED','@event_user joined the attackers'),
  ('ATTACKER_MESSAGE','@event_user: @chat_message'),
  ('ATTACKER_MUTANT_CREATED','@event_user created a mutant'),
  ('ATTACKER_MUTANT_ERROR','@event_user created a mutant that errored'),
  ('ATTACKER_MUTANT_KILLED_EQUIVALENT','@event_user proved a mutant non-equivalent'),
  ('ATTACKER_MUTANT_SURVIVED','@event_user created a mutant that survived'),
  ('DEFENDER_JOINED','@event_user joined the defenders!'),
  ('DEFENDER_KILLED_MUTANT','@event_user killed a mutant'),
  ('DEFENDER_MESSAGE','@event_user: @chat_message'),
  ('DEFENDER_MUTANT_CLAIMED_EQUIVALENT','@event_user claimed a mutant equivalent'),
  ('DEFENDER_MUTANT_EQUIVALENT','@event_user caught an equivalence'),
  ('DEFENDER_TEST_CREATED','@event_user created a test'),
  ('DEFENDER_TEST_ERROR','@event_user created a test that errored'),
  ('DEFENDER_TEST_READY','Test by @event_user is ready'),
  ('GAME_CREATED','Game created'),('GAME_FINISHED','Game Over!'),
  ('GAME_GRACE_ONE','The game is entering grace period one.'),
  ('GAME_GRACE_TWO','The game is entering grace period two.'),
  ('GAME_MESSAGE','@event_user: @chat_message'),
  ('GAME_MESSAGE_ATTACKER','@event_user: @chat_message'),
  ('GAME_MESSAGE_DEFENDER','@event_user: @chat_message'),
  ('GAME_PLAYER_LEFT','@event_user left the game'),
  ('GAME_STARTED','The game has started!');
/*!40000 ALTER TABLE `event_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Leaderboard View
--
CREATE OR REPLACE VIEW `view_attackers`
  AS
    SELECT
      PA.user_id,
      count(M.Mutant_ID) AS NMutants,
      sum(M.Points)      AS AScore
    FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID
    GROUP BY PA.user_id;

CREATE OR REPLACE VIEW `view_defenders`
  AS
    SELECT
      PD.user_id,
      count(T.Test_ID)     AS NTests,
      sum(T.Points)        AS DScore,
      sum(T.MutantsKilled) AS NKilled
    FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID
    GROUP BY PD.user_id;

CREATE OR REPLACE VIEW `view_leaderboard`
AS
  SELECT
    U.username                            AS username,
    IFNULL(NMutants, 0)                   AS NMutants,
    IFNULL(AScore, 0)                     AS AScore,
    IFNULL(NTests, 0)                     AS NTests,
    IFNULL(DScore, 0)                     AS DScore,
    IFNULL(NKilled, 0)                    AS NKilled,
    IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore
  FROM users U
    LEFT JOIN view_attackers ON U.user_id = view_attackers.user_id
    LEFT JOIN view_defenders ON U.user_id = view_defenders.user_id
  WHERE U.user_id > 2; -- Ignore automated players

--
-- Automated attacker and defender
--

INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`) VALUES (1, 'Mutator', 'AI_ATTACKER_INACCESSIBLE', 'codedef_mutator@sheffield.ac.uk');
INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`) VALUES (2, 'TestGen', 'AI_DEFENDER_INACCESSIBLE', 'codedef_testgen@sheffield.ac.uk');

-- Event to activate multiplayer game
-- SET @@global.event_scheduler = 1;

--
-- Handling equivalences after time expiration
--
DROP PROCEDURE IF EXISTS proc_multiplayer_task;

DELIMITER //
CREATE PROCEDURE proc_multiplayer_task()
BEGIN
  -- Activate games when its start time has passed and there are sufficient players
  UPDATE games as g
    INNER JOIN (SELECT gatt.ID, sum(case when Role = 'ATTACKER' then 1 else 0 end) nAttackers, sum(case when Role = 'DEFENDER' then 1 else 0 end) nDefenders
                FROM games as gatt LEFT JOIN players ON gatt.ID=players.Game_ID AND players.Active=TRUE GROUP BY gatt.ID) as nplayers
    ON g.ID=nplayers.ID
  SET g.State='ACTIVE'
  WHERE g.Mode='PARTY' AND g.State='CREATED' AND g.Start_Time<=CURRENT_TIMESTAMP
        AND g.Attackers_Needed <= nplayers.nAttackers AND g.Defenders_Needed <= nplayers.nDefenders;

  UPDATE games SET State='GRACE_ONE'
  WHERE Mode='PARTY' AND State='ACTIVE' AND Finish_Time<=DATE_ADD(NOW(), INTERVAL 1 HOUR);

  UPDATE games SET State='GRACE_TWO'
  WHERE Mode='PARTY' AND State='GRACE_ONE' AND Finish_Time<=DATE_ADD(NOW(), INTERVAL 45 MINUTE);

  UPDATE games AS g
  LEFT JOIN mutants AS m ON m.Game_ID = g.ID
  LEFT JOIN equivalences AS e ON e.Mutant_ID = m.Mutant_ID
  SET State='FINISHED', e.Expired = 1
  WHERE Mode='PARTY' AND (State='GRACE_TWO' OR (State='FINISHED' AND m.Equivalent='PENDING_TEST')) AND Finish_Time<=NOW();

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
