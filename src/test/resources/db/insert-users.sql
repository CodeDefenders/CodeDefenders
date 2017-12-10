-- MySQL dump 10.13  Distrib 5.7.17, for macos10.12 (x86_64)
--
-- Host: localhost    Database: defender
-- ------------------------------------------------------
-- Server version	5.7.17

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
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (7,'bot','$2a$10$b.48vxZXBdomntwjZaWeoeER30AqOZrnK46.BQZf0pg9RnH1J7yCW','bot@gw.uni-passau.de',0),(8,'demoattacker','$2a$10$znCwI.aTqrb9.t396vG9g.pxYSjqawV6BbjT/DNQtON34xSsNeXGG','demoattacker@gw.uni-passau.de',0),(9,'demodefender','$2a$10$6hqPNfn2FVyMiL39C5lRnOwgolCXWlv7eXxAFS8n0mdDiRjPCUPD2','demodefender@gw.uni-passau.de',0),(10,'demoattacker2','$2a$10$VswuhewotUxSixJydX/tGuhf3WiMn8MUOxAFtr6D.Kkz2QRhLGX/y','demoattacker2@gw.uni-passau.de',0),(11,'demodefender2','$2a$10$rFzDZBFMwE73k9Yamt2qge080epdep03f6p6CBrnJrw5wubNnzWAy','demodefender2@gw.uni-passau.de',0),(12,'creator','$2a$10$0kx7uSwKP7whp3rDNRG5Q.0Yi2xmiGgnEEx09BhOlaFPz9E06O0/2','creator@gw.uni-passau.de',0);
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

-- Dump completed on 2017-12-10 21:33:40
