-- --------------------------------------------------------
-- Host:                         stusql.dcs.shef.ac.uk
-- Server version:               5.0.95 - Source distribution
-- Server OS:                    redhat-linux-gnu
-- HeidiSQL Version:             8.3.0.4694
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping database structure for team007
CREATE DATABASE IF NOT EXISTS `team007` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `team007`;


-- Dumping structure for table team007.Keywords
CREATE TABLE IF NOT EXISTS `Keywords` (
  `wordId` int(11) NOT NULL auto_increment,
  `word` varchar(30) NOT NULL,
  `lastUpdated` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`wordId`),
  UNIQUE KEY `word` (`word`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table team007.Locations
CREATE TABLE IF NOT EXISTS `Locations` (
  `locId` varchar(25) NOT NULL,
  `name` varchar(50) default NULL,
  `imageUrl` tinytext,
  `address` tinytext,
  `city` varchar(25) default NULL,
  `websiteUrl` tinytext,
  `description` text,
  `lastUpdated` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`locId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table team007.UserKeyword
CREATE TABLE IF NOT EXISTS `UserKeyword` (
  `wordId` int(11) NOT NULL,
  `userId` bigint(20) NOT NULL,
  `count` int(11) NOT NULL,
  `lastUpdated` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`wordId`,`userId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table team007.UserLocation
CREATE TABLE IF NOT EXISTS `UserLocation` (
  `userId` bigint(20) NOT NULL,
  `locId` varchar(25) NOT NULL,
  `lastUpdated` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`userId`,`locId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table team007.Users
CREATE TABLE IF NOT EXISTS `Users` (
  `userId` bigint(20) NOT NULL,
  `fullName` varchar(50) default NULL,
  `screenName` varchar(50) default NULL,
  `hometown` varchar(50) default NULL,
  `profileImgUrl` tinytext,
  `bigProfileImgUrl` tinytext,
  `bannerImgUrl` tinytext,
  `description` text,
  `lastUpdated` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`userId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table team007.UserUserContact
CREATE TABLE IF NOT EXISTS `UserUserContact` (
  `userA` bigint(20) NOT NULL,
  `userB` bigint(20) NOT NULL,
  `lastUpdated` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`userA`,`userB`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
