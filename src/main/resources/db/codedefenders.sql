-- This is an edited MySQL dump
-- MySQL dump 10.16  Distrib 10.1.36-MariaDB, for Linux (x86_64)

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP TABLE IF EXISTS `killmapjob`;

CREATE TABLE killmapjob
(
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `Game_ID` int(11),
  `Class_ID` int(11),
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`)
) AUTO_INCREMENT=1;

DROP TABLE IF EXISTS `test_smell`;
CREATE TABLE test_smell (
	`test_ID` int(11),
	`smell_name` VARCHAR(500),
	PRIMARY KEY (test_ID, smell_name)
);

--
-- Table structure for table `settings`
--
DROP TABLE IF EXISTS `settings`;
CREATE TABLE `settings` (
  `name` varchar(50) NOT NULL,
  `type` enum('STRING_VALUE','INT_VALUE','BOOL_VALUE') DEFAULT NULL,
  `STRING_VALUE` text,
  `INT_VALUE` int(11) DEFAULT NULL,
  `BOOL_VALUE` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`name`)
);

--
-- Default settings
--

INSERT INTO settings (name, type, STRING_VALUE, INT_VALUE, BOOL_VALUE) VALUES
  ('SHOW_PLAYER_FEEDBACK', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('REGISTRATION', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('CLASS_UPLOAD', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('GAME_CREATION', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('GAME_JOINING', 'BOOL_VALUE', NULL, NULL, TRUE),
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
  ('EMAIL_PASSWORD', 'STRING_VALUE', '', NULL, NULL),
  ('AUTOMATIC_KILLMAP_COMPUTATION', 'BOOL_VALUE', NULL, NULL, FALSE);

--
-- Table structure for table `ratings`
--

DROP TABLE IF EXISTS `ratings`;
CREATE TABLE `ratings` (
  `User_ID` int(11) NOT NULL DEFAULT '-1',
  `Game_ID` int(11) NOT NULL DEFAULT '-1',
  `type` varchar(128) DEFAULT NULL,
  `value` int(11) NOT NULL DEFAULT '0',
  UNIQUE KEY `game_user_type_unique` (`User_ID`,`Game_ID`,`type`),
  KEY `fk_ratings_gameID_games` (`Game_ID`),
  CONSTRAINT `fk_ratings_gameID_games` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_ratings_userID_users` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE CASCADE ON UPDATE CASCADE
);

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes` (
  `Class_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Name` varchar(255) NOT NULL,
  `JavaFile` varchar(255) NOT NULL,
  `ClassFile` varchar(255) NOT NULL,
  `Alias` varchar(50) NOT NULL,
  `AiPrepared` tinyint(1) DEFAULT '0',
  `RequireMocking` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`Class_ID`),
  UNIQUE KEY `classes_Alias_uindex` (`Alias`)
) AUTO_INCREMENT=100;

--
-- Table structure for table 'dependencies'
--

DROP TABLE IF EXISTS `dependencies`;
CREATE TABLE `dependencies` (
  `Dependency_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Class_ID` int(11) NOT NULL,
  `JavaFile` varchar(255) NOT NULL,
  `ClassFile` varchar(255) NOT NULL,
  PRIMARY KEY (`Dependency_ID`),
  KEY `fk_classId_dependencies` (`Class_ID`),
  CONSTRAINT `fk_classId_dependencies` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) AUTO_INCREMENT=100;

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
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
  `Start_Time` timestamp NOT NULL DEFAULT '1970-02-02 01:01:01',
  `Finish_Time` timestamp NOT NULL DEFAULT '1970-02-02 01:01:01',
  `MaxAssertionsPerTest` int(11) NOT NULL DEFAULT '2',
  `MutantValidator` enum('STRICT','MODERATE','RELAXED') NOT NULL DEFAULT 'MODERATE',
  `MarkUncovered` tinyint(1) NOT NULL DEFAULT '0',
  `ChatEnabled` tinyint(1) DEFAULT '1',
  `Attackers_Limit` int(11) DEFAULT '0',
  `Defenders_Limit` int(11) DEFAULT '0',
  `State` enum('CREATED','ACTIVE','FINISHED','GRACE_ONE','GRACE_TWO') DEFAULT 'CREATED',
  `CurrentRound` tinyint(4) NOT NULL DEFAULT '1',
  `FinalRound` tinyint(4) NOT NULL DEFAULT '5',
  `ActiveRole` enum('ATTACKER','DEFENDER') NOT NULL DEFAULT 'ATTACKER',
  `Mode` enum('SINGLE','DUEL','PARTY','UTESTING') NOT NULL DEFAULT 'DUEL',
  `RequiresValidation` tinyint(1) NOT NULL DEFAULT '0',
  `IsAIDummyGame` tinyint(1) NOT NULL DEFAULT '0',
  `HasKillMap` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ID`),
  KEY `fk_creatorId_idx` (`Creator_ID`),
  KEY `fk_className_idx` (`Class_ID`),
  CONSTRAINT `fk_classId` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_className` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_creatorId` FOREIGN KEY (`Creator_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) AUTO_INCREMENT=100;

--
-- Dummy game for upload of tests and mutants together with a class
--

INSERT INTO games (ID, State)
VALUES (-1, 'FINISHED');

--
-- Table structure for table `killmap`
--

DROP TABLE IF EXISTS `killmap`;
CREATE TABLE `killmap` (
  `Class_ID` int(11) NOT NULL,
  `Game_ID` int(11) DEFAULT NULL,
  `Test_ID` int(11) NOT NULL,
  `Mutant_ID` int(11) NOT NULL,
  `Status` enum('KILL','NO_KILL','NO_COVERAGE','ERROR','UNKNOWN') NOT NULL,
  PRIMARY KEY (`Test_ID`,`Mutant_ID`),
  KEY `fk_killmap_classId` (`Class_ID`),
  KEY `fk_killmap_gameId` (`Game_ID`),
  KEY `fk_killmap_mutantId` (`Mutant_ID`),
  CONSTRAINT `fk_killmap_classId` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_killmap_gameId` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_killmap_mutantId` FOREIGN KEY (`Mutant_ID`) REFERENCES `mutants` (`Mutant_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_killmap_testId` FOREIGN KEY (`Test_ID`) REFERENCES `tests` (`Test_ID`) ON DELETE CASCADE ON UPDATE CASCADE
);

--
-- Table structure for table `mutants`
--

DROP TABLE IF EXISTS `mutants`;
CREATE TABLE `mutants` (
  `Mutant_ID` int(11) NOT NULL AUTO_INCREMENT,
  `JavaFile` varchar(255) NOT NULL,
  `MD5` char(32) NOT NULL,
  `ClassFile` varchar(255) DEFAULT NULL,
  `Alive` tinyint(1) NOT NULL DEFAULT '1',
  `Game_ID` int(11) NOT NULL,
  `Class_ID` int(11) DEFAULT NULL,
  `RoundCreated` int(11) NOT NULL,
  `RoundKilled` int(11) DEFAULT NULL,
  `Equivalent` enum('ASSUMED_NO','PENDING_TEST','DECLARED_YES','ASSUMED_YES','PROVEN_NO') NOT NULL DEFAULT 'ASSUMED_NO',
  `Player_ID` int(11) DEFAULT NULL,
  `NumberAiKillingTests` int(11) DEFAULT '0', -- If an original ai mutant, kill count. Number of killing tests in game otherwise.
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Points` int(11) DEFAULT '0',
  `MutatedLines` varchar(255),
  PRIMARY KEY (`Mutant_ID`),
  UNIQUE KEY `mutants_Game_ID_Class_ID_MD5_key` (`Game_ID`,`Class_ID`,`MD5`),
  KEY `fk_gameId_idx` (`Game_ID`),
  KEY `fk_playerId_idx` (`Player_ID`),
  KEY `fk_classId_muts` (`Class_ID`),
  CONSTRAINT `fk_classId_muts` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_gameId_muts` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_playerId_muts` FOREIGN KEY (`Player_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) AUTO_INCREMENT=100;

--
-- Mapping between mutants and the class the mutant is uploaded together with
--

DROP TABLE IF EXISTS `mutant_uploaded_with_class`;
CREATE TABLE `mutant_uploaded_with_class` (
  `Class_ID` int(11),
  `Mutant_ID`  int(11),
  PRIMARY KEY (`Class_ID`, `Mutant_ID`),
  FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`),
  FOREIGN KEY (`Mutant_ID`) REFERENCES mutants (`Mutant_ID`)
);

--
-- Table structure for table `players`
--

DROP TABLE IF EXISTS `players`;
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
) AUTO_INCREMENT=100;

--
-- Dummy player for upload of tests and mutants together with a class.
-- Dummy player attacker (for mutants) and dummy player defender (for tests).
-- The corresponding users are added under the `users` table.
--

INSERT INTO `players` (`ID`, `User_ID`, `Game_ID`)
VALUES (-1, -1, -1),
       ( 3,  3, -1),
       ( 4,  4, -1);

--
-- Table structure for table `targetexecutions`
--

DROP TABLE IF EXISTS `targetexecutions`;
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
) AUTO_INCREMENT=100;

--
-- Table structure for table `tests`
--

DROP TABLE IF EXISTS `tests`;
CREATE TABLE `tests` (
  `Test_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Game_ID` int(11) NOT NULL,
  `Class_ID` int(11) DEFAULT NULL,
  `JavaFile` varchar(255) NOT NULL,
  `ClassFile` varchar(255) DEFAULT NULL,
  `RoundCreated` int(11) NOT NULL,
  `MutantsKilled` int(11) DEFAULT '0',
  `Player_ID` int(11) NOT NULL,
  `NumberAiMutantsKilled` int(11) DEFAULT '0', -- If an original ai test, kill count. Number of kills in game otherwise.
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Lines_Covered` longtext,
  `Lines_Uncovered` longtext,
  `Points` int(11) DEFAULT '0',
  PRIMARY KEY (`Test_ID`),
  KEY `fk_playerId_idx` (`Player_ID`),
  KEY `fk_gameId_tests_idx` (`Game_ID`),
  KEY `fk_playerId_tests_idx` (`Player_ID`),
  KEY `fk_classId_tests` (`Class_ID`),
  CONSTRAINT `fk_classId_tests` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_gameId_tests` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_playerId_tests` FOREIGN KEY (`Player_ID`) REFERENCES `players` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) AUTO_INCREMENT=100;

--
-- Mapping between test and the class the test is uploaded together with
--

DROP TABLE IF EXISTS `test_uploaded_with_class`;
CREATE TABLE `test_uploaded_with_class` (
  `Class_ID` int(11),
  `Test_ID`  int(11),
  PRIMARY KEY (`Class_ID`, `Test_ID`),
  FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`),
  FOREIGN KEY (`Test_ID`) REFERENCES tests (`Test_ID`)
);
--
-- Table structure for table `usedaimutants`
--

DROP TABLE IF EXISTS `usedaimutants`;
CREATE TABLE `usedaimutants` (
  `UsedMutant_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Value` int(11) DEFAULT NULL,
  `Game_ID` int(11) NOT NULL,
  PRIMARY KEY (`UsedMutant_ID`),
  KEY `fk_gameId_ai_mutants_idx` (`Game_ID`),
  CONSTRAINT `fk_gameId_ai_mutants` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) AUTO_INCREMENT=100;

--
-- Table structure for table `usedaitests`
--

DROP TABLE IF EXISTS `usedaitests`;
CREATE TABLE `usedaitests` (
  `UsedTest_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Value` int(11) DEFAULT NULL,
  `Game_ID` int(11) NOT NULL,
  PRIMARY KEY (`UsedTest_ID`),
  KEY `fk_gameId_ai_test_idx` (`Game_ID`),
  CONSTRAINT `fk_gameId_ai_test` FOREIGN KEY (`Game_ID`) REFERENCES `games` (`ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) AUTO_INCREMENT=100;

--
-- Table structure for table `registeredEmails`
--

DROP TABLE IF EXISTS `registeredEmails`;
CREATE TABLE `registeredEmails` (
  `email` varchar(150) NOT NULL,
  PRIMARY KEY (`email`)
) ENGINE=InnoDB;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `User_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Username` varchar(20) NOT NULL,
  `Password` char(60) NOT NULL,
  `Email` varchar(150) NOT NULL,
  `Validated` tinyint(1) NOT NULL DEFAULT '0',
  `Active` tinyint(1) NOT NULL DEFAULT '1',
  `pw_reset_timestamp` timestamp NULL DEFAULT NULL,
  `pw_reset_secret` varchar(254) DEFAULT NULL,
  PRIMARY KEY (`User_ID`),
  UNIQUE KEY `users_email_index` (`Email`)
) AUTO_INCREMENT=100;

--
-- Trigger that validates a new user if the email address is already validated.
--

DELIMITER $$
CREATE TRIGGER ins_users
  BEFORE INSERT ON `users`
  FOR EACH ROW BEGIN
  IF (NEW.Email IN (SELECT * FROM registeredEmails)) THEN
    SET NEW.Validated = TRUE;
  END IF;
END$$
DELIMITER ;

--
-- Automated attacker and defender.
-- Dummy user for upload of tests and mutants together with a class.
-- Dummy attacker (for mutants) and dummy defender (for tests).
--

INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`)
VALUES (1, 'Mutator', 'AI_ATTACKER_INACCESSIBLE', 'codedef_mutator@sheffield.ac.uk'),
       (2, 'TestGen', 'AI_DEFENDER_INACCESSIBLE', 'codedef_testgen@sheffield.ac.uk'),
       (-1, 'dummy_user', 'DUMMY_USER_INACCESSIBLE', 'user@dummy.com'),
       (3, 'System Attacker', 'DUMMY_ATTACKER_INACCESSIBLE', 'attacker@dummy.com'),
       (4, 'System Defender', 'DUMMY_DEFENDER_INACCESSIBLE', 'defender@dummy.com');

--
-- Table structure for table `sessions`
--

DROP TABLE IF EXISTS `sessions`;
CREATE TABLE `sessions` (
  `Session_ID` int(11) NOT NULL AUTO_INCREMENT,
  `User_ID` int(11) NOT NULL,
  `IP_Address` varchar(100) NOT NULL,
  `Timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`Session_ID`),
  KEY `fk_userId_sessions` (`User_ID`),
  CONSTRAINT `fk_userId_sessions` FOREIGN KEY (`User_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION
) AUTO_INCREMENT=100;

--
-- Table structure for table `equivalences`
--

DROP TABLE IF EXISTS `equivalences`;
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
) AUTO_INCREMENT=100;

--
-- Table structure for table `events`
--

DROP TABLE IF EXISTS `events`;
CREATE TABLE `events` (
  `Event_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Game_ID` int(11) DEFAULT NULL,
  `Player_ID` int(11) DEFAULT NULL,
  `Event_Message` varchar(255) DEFAULT NULL,
  `Event_Type` varchar(45) DEFAULT NULL,
  `Event_Status` varchar(45) DEFAULT NULL,
  `Timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`Event_ID`)
) AUTO_INCREMENT=100;

--
-- Table structure for table `event_chat`
--

DROP TABLE IF EXISTS `event_chat`;
CREATE TABLE `event_chat` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `Event_Id` int(11) DEFAULT NULL,
  `Message` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`Id`)
) AUTO_INCREMENT=100;

--
-- Table structure for table `event_messages`
--

DROP TABLE IF EXISTS `event_messages`;
CREATE TABLE `event_messages` (
  `Event_Type` varchar(45) NOT NULL,
  `Message` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`Event_Type`)
);

--
-- Dumping data for table `event_messages`
--

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

--
-- Leaderboard Views
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
    WHERE U.user_id >= 100; -- Ignore automated players



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



/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
