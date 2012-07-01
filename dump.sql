-- MySQL dump 10.11
--
-- Host: localhost    Database: freekyweb
-- ------------------------------------------------------
-- Server version	5.0.51a-24+lenny5

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
-- Dumping data for table `AccountType`
--

LOCK TABLES `AccountType` WRITE;
/*!40000 ALTER TABLE `AccountType` DISABLE KEYS */;
INSERT INTO `AccountType` (`name`, `rLogin`, `description`, `id`, `rAdministrateUsers`) VALUES ('Administrator',1,'darf alles',1,1),('User',1,'standard',2,0),('Unconfirmed',0,'noch nicht dabei',3,0);
/*!40000 ALTER TABLE `AccountType` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `LoginAttempt`
--

LOCK TABLES `LoginAttempt` WRITE;
/*!40000 ALTER TABLE `LoginAttempt` DISABLE KEYS */;
INSERT INTO `LoginAttempt` (`ip`, `success`, `validated`, `id`, `time`, `userId`) VALUES ('89.31.140.13',1,0,1,'2011-10-08 13:35:27',1),('89.31.140.30',1,0,2,'2011-10-08 13:37:23',1),('89.31.140.29',1,0,3,'2011-10-08 13:38:44',1),('89.31.140.31',1,0,4,'2011-10-08 14:10:17',1),('89.31.140.17',1,0,5,'2011-11-26 17:39:47',1),('127.0.0.1',1,0,6,'2011-12-28 00:17:34',1),('127.0.0.1',1,0,7,'2011-12-28 13:58:17',1),('89.31.140.31',1,0,8,'2011-12-28 14:09:33',1);
/*!40000 ALTER TABLE `LoginAttempt` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `Project`
--

LOCK TABLES `Project` WRITE;
/*!40000 ALTER TABLE `Project` DISABLE KEYS */;
INSERT INTO `Project` (`name`, `description`, `text`, `id`) VALUES ('test','This is a test Project','*Test* whats going on in here?:)',1),('Test Nummer 2','Dies ist das Zweite Projekt !','h2. Test Nummer 2\n\nDies ist der Weg zur *+Weltherschaft!+*',2);
/*!40000 ALTER TABLE `Project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `User`
--

LOCK TABLES `User` WRITE;
/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` (`name`, `email`, `id`, `passwordhash`, `passwordsalt`, `registrationdate`, `accounttypeId`) VALUES ('Freeky','test@test.de',1,'NZKz3fTuTAvh/FccjyUfbpCxF6g=','BSIWFE3CORHLIXC5','2011-10-08 13:33:55',1);
/*!40000 ALTER TABLE `User` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-12-31 11:44:03
