CREATE DATABASE  IF NOT EXISTS `codedefenders` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `codedefenders`;
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

--
-- Dumping data for table `attackers`
--

LOCK TABLES `attackers` WRITE;
/*!40000 ALTER TABLE `attackers` DISABLE KEYS */;
/*!40000 ALTER TABLE `attackers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (1,'ArrayExamples','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.class'),(2,'ArrayExamples','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.class'),(3,'ArrayExamples','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.class'),(4,'ArrayExamples','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.class'),(5,'ArrayExamples','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.class'),(6,'ArrayExamples','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.class'),(7,'ArrayExamples','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\sources\\ArrayExamples.class');
/*!40000 ALTER TABLE `classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `defenders`
--

LOCK TABLES `defenders` WRITE;
/*!40000 ALTER TABLE `defenders` DISABLE KEYS */;
/*!40000 ALTER TABLE `defenders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `games`
--

LOCK TABLES `games` WRITE;
/*!40000 ALTER TABLE `games` DISABLE KEYS */;
INSERT INTO `games` VALUES (1,NULL,1,1,3,'ATTACKER',1,'CREATED','EASY','DUEL','2016-06-02 15:01:32'),(2,2,1,2,3,'ATTACKER',1,'ACTIVE','HARD','DUEL','2016-06-02 15:06:50'),(3,NULL,1,1,3,'ATTACKER',1,'CREATED','HARD','DUEL','2016-06-02 15:16:55'),(4,1,2,1,3,'DEFENDER',1,'ACTIVE','HARD','DUEL','2016-06-02 17:50:21');
/*!40000 ALTER TABLE `games` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `multiplayer_games`
--

LOCK TABLES `multiplayer_games` WRITE;
/*!40000 ALTER TABLE `multiplayer_games` DISABLE KEYS */;
/*!40000 ALTER TABLE `multiplayer_games` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `mutant_killers`
--

LOCK TABLES `mutant_killers` WRITE;
/*!40000 ALTER TABLE `mutant_killers` DISABLE KEYS */;
/*!40000 ALTER TABLE `mutant_killers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `mutants`
--

LOCK TABLES `mutants` WRITE;
/*!40000 ALTER TABLE `mutants` DISABLE KEYS */;
INSERT INTO `mutants` VALUES (1,'C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\mutants\\2\\1\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\mutants\\2\\1\\ArrayExamples.class',1,2,1,NULL,'ASSUMED_NO',2,'2016-06-02 15:19:15',NULL),(2,'C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\mutants\\4\\1\\ArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\mutants\\4\\1\\ArrayExamples.class',1,4,1,NULL,'ASSUMED_NO',1,'2016-06-02 17:50:57',NULL);
/*!40000 ALTER TABLE `mutants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `targetexecutions`
--

LOCK TABLES `targetexecutions` WRITE;
/*!40000 ALTER TABLE `targetexecutions` DISABLE KEYS */;
INSERT INTO `targetexecutions` VALUES (1,NULL,1,'COMPILE_MUTANT','SUCCESS','','2016-06-02 15:19:15'),(2,NULL,2,'COMPILE_MUTANT','SUCCESS','','2016-06-02 17:50:57'),(3,1,NULL,'COMPILE_TEST','SUCCESS','','2016-06-02 18:00:01'),(4,1,NULL,'TEST_ORIGINAL','SUCCESS','','2016-06-02 18:00:01'),(5,1,1,'TEST_MUTANT','SUCCESS','','2016-06-02 18:00:02');
/*!40000 ALTER TABLE `targetexecutions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `tests`
--

LOCK TABLES `tests` WRITE;
/*!40000 ALTER TABLE `tests` DISABLE KEYS */;
INSERT INTO `tests` VALUES (1,2,'C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\tests\\2\\1\\TestArrayExamples.java','C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0\\webapps\\ROOT\\WEB-INF\\data\\tests\\2\\1\\TestArrayExamples.class',1,0,1,'2016-06-02 18:00:01',NULL);
/*!40000 ALTER TABLE `tests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'tom','$2a$10$ObbFuSbAwpnelkLJRC7R4O/OQNW8.0hhIfUKslsaxGHHp9X1vXYFS'),(2,'t','$2a$10$qXDKpli3e3LsJObuc10jY.Ox8F40j.XLLM226fCFy3QYIXTg8w2SW');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-06-03 16:36:55
