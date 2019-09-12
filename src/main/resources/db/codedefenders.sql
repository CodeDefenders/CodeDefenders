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
	`Test_ID` int(11),
	`smell_name` VARCHAR(191),
	PRIMARY KEY (Test_ID, smell_name)
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
  ('AUTOMATIC_KILLMAP_COMPUTATION', 'BOOL_VALUE', NULL, NULL, FALSE),
  ('ALLOW_USER_PROFILE', 'BOOL_VALUE', NULL, NULL, TRUE),
  ('PRIVACY_NOTICE', 'STRING_VALUE', '', NULL, NULL);

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
  `Puzzle` tinyint(1) NOT NULL DEFAULT '0',
  `Active` tinyint(1) NOT NULL DEFAULT '1',
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
  `ChatEnabled` tinyint(1) DEFAULT '1',
  `Attackers_Limit` int(11) DEFAULT '0',
  `Defenders_Limit` int(11) DEFAULT '0',
  `State` enum('CREATED','ACTIVE','FINISHED','GRACE_ONE','GRACE_TWO','SOLVED','FAILED') DEFAULT 'CREATED',
  `CurrentRound` tinyint(4) NOT NULL DEFAULT '1',
  `FinalRound` tinyint(4) NOT NULL DEFAULT '5',
  `ActiveRole` enum('ATTACKER','DEFENDER') NOT NULL DEFAULT 'ATTACKER',
  `Mode` enum('SINGLE','DUEL','PARTY','UTESTING','PUZZLE') NOT NULL DEFAULT 'PARTY',
  `RequiresValidation` tinyint(1) NOT NULL DEFAULT '0',
  `IsAIDummyGame` tinyint(1) NOT NULL DEFAULT '0',
  `HasKillMap` tinyint(1) NOT NULL DEFAULT '0',
  `CapturePlayersIntention` tinyint(1) NOT NULL DEFAULT '0',
  `Puzzle_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  KEY `fk_creatorId_idx` (`Creator_ID`),
  KEY `fk_className_idx` (`Class_ID`),
  KEY `games_puzzles_Puzzle_ID_fk` (`Puzzle_ID`),
  CONSTRAINT `fk_classId` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_className` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_creatorId` FOREIGN KEY (`Creator_ID`) REFERENCES `users` (`User_ID`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `hasKillMap` CHECK (HasKillMap = 0 OR State = 'FINISHED'), -- only finished games can have a killmap
  CONSTRAINT `games_puzzles_Puzzle_ID_fk` FOREIGN KEY (`Puzzle_ID`) REFERENCES `puzzles` (`Puzzle_ID`) ON DELETE CASCADE ON UPDATE CASCADE
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
-- Table structure for table `puzzle_chapters`
--

DROP TABLE IF EXISTS `puzzle_chapters`;
CREATE TABLE `puzzle_chapters` (
  `Chapter_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Position` int(11) DEFAULT NULL,
  `Title` varchar(100) DEFAULT NULL,
  `Description` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`Chapter_ID`),
  UNIQUE KEY `puzzles_chapter_Position_unique` (`Position`)
) AUTO_INCREMENT=100;

--
-- Table structure for table `puzzles`
--

DROP TABLE IF EXISTS `puzzles`;
CREATE TABLE `puzzles` (
  `Puzzle_ID` int(11) NOT NULL AUTO_INCREMENT,
  `Class_ID` int(11) NOT NULL,
  `Active_Role` enum('ATTACKER','DEFENDER') NOT NULL,
  `Level` enum('EASY','HARD') DEFAULT 'HARD',
  `Max_Assertions` int(11) NOT NULL DEFAULT '2',
  `Mutant_Validator_Level` enum('STRICT','MODERATE','RELAXED') NOT NULL DEFAULT 'MODERATE',
  `Editable_Lines_Start` int(11) DEFAULT NULL,
  `Editable_Lines_End` int(11) DEFAULT NULL,
  `Chapter_ID` int(11) DEFAULT NULL,
  `Position` int(11) DEFAULT NULL,
  `Title` varchar(100) DEFAULT NULL,
  `Description` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`Puzzle_ID`),
  UNIQUE KEY `puzzles_Chapter_ID_Position_unique` (`Chapter_ID`,`Position`),
  KEY `puzzles_classes_Class_ID_fk` (`Class_ID`),
  CONSTRAINT `puzzles_classes_Class_ID_fk` FOREIGN KEY (`Class_ID`) REFERENCES `classes` (`Class_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `puzzles_puzzle_chapters_Level_fk` FOREIGN KEY (`Chapter_ID`) REFERENCES `puzzle_chapters` (`Chapter_ID`) ON DELETE SET NULL ON UPDATE CASCADE
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

DROP TABLE IF EXISTS `intention`;
CREATE TABLE `intention` (
  `Intention_ID`       int(11) NOT NULL AUTO_INCREMENT,
  `Test_ID`            int(11),
  `Mutant_ID`          int(11),
  `Game_ID`            int(11) NOT NULL,
  `Target_Mutants`     longtext,
  `Target_Lines`       longtext,
  `Target_Mutant_Type` longtext,
  PRIMARY KEY (`Intention_ID`)
) AUTO_INCREMENT = 1;
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
  `AllowContact` tinyint(1) NOT NULL DEFAULT '0',
  `KeyMap` enum('DEFAULT','SUBLIME','VIM','EMACS') NOT NULL DEFAULT 'DEFAULT',
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

CREATE OR REPLACE VIEW `view_active_classes` AS
SELECT *
FROM classes
WHERE Active = 1;

CREATE OR REPLACE VIEW `view_playable_classes` AS
SELECT *
FROM `view_active_classes`
WHERE Puzzle = 0;

CREATE OR REPLACE VIEW `view_puzzle_classes` AS
SELECT *
FROM `view_active_classes`
WHERE Puzzle = 1;

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

CREATE OR REPLACE VIEW `view_mutants_with_user` AS
SELECT mutants.*, users.*
FROM mutants
       LEFT JOIN players ON players.ID = mutants.Player_ID
       LEFT JOIN users ON players.User_ID = users.User_ID;

CREATE OR REPLACE VIEW `view_valid_mutants` AS
SELECT *
FROM view_mutants_with_user
WHERE ClassFile IS NOT NULL;

CREATE OR REPLACE VIEW `view_players` AS
SELECT *
FROM players
WHERE `ID` >= 100;

CREATE OR REPLACE VIEW `view_valid_users` AS
SELECT * FROM `users`
WHERE `User_ID` >= 5
  AND Active = 1;

CREATE OR REPLACE VIEW `view_players_with_userdata` AS
SELECT p.*,
       u.Password     AS usersPassword,
       u.Username     AS usersUsername,
       u.Email        AS usersEmail,
       u.Validated    AS usersValidated,
       u.Active       AS usersActive,
       u.AllowContact AS usersAllowContact,
       u.KeyMap       AS usersKeyMap
FROM players AS p,
     view_valid_users AS u
WHERE p.User_ID = u.User_ID;

CREATE OR REPLACE VIEW `view_valid_tests` AS
SELECT *
FROM tests
WHERE tests.ClassFile IS NOT NULL
  AND EXISTS(
    SELECT *
    FROM targetexecutions ex
    WHERE ex.Test_ID = tests.Test_ID
      AND ex.Target = 'TEST_ORIGINAL'
      AND ex.Status = 'SUCCESS'
  );

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
    FROM view_valid_users U
      LEFT JOIN view_attackers ON U.user_id = view_attackers.user_id
      LEFT JOIN view_defenders ON U.user_id = view_defenders.user_id;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
