-- .\mysqldump.exe -uroot -p --database --skip-add-drop-table --no-data ksfx > ksfxnodata.sql
-- MySQL dump 10.13  Distrib 5.6.17, for Win64 (x86_64)
--
-- Host: localhost    Database: ksfx
-- ------------------------------------------------------
-- Server version	5.6.17

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
-- Current Database: `ksfx`
--

-- CREATE DATABASE /*!32312 IF NOT EXISTS*/ `ksfx` /*!40100 DEFAULT CHARACTER SET utf8 */;

-- USE `ksfx`;

--
-- Table structure for table `activity`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) DEFAULT NULL,
  `cron_schedule` varchar(512) DEFAULT NULL,
  `groovy_code` longtext,
  `requires_approval` bit(1) DEFAULT NULL,
  `cron_schedule_enabled` bit(1) DEFAULT NULL,
  `activity_approval_strategy` bigint(20) DEFAULT NULL,
  `activity_category` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_approval_strategy_idx` (`activity_approval_strategy`),
  KEY `fk_activity_category_idx` (`activity_category`),
  CONSTRAINT `fk_activity_category` FOREIGN KEY (`activity_category`) REFERENCES `activity_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_approval_strategy` FOREIGN KEY (`activity_approval_strategy`) REFERENCES `activity_approval_strategy` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_approval_strategy`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_approval_strategy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_instance`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `finished` datetime DEFAULT NULL,
  `started` datetime DEFAULT NULL,
  `activity` bigint(20) DEFAULT NULL,
  `approved` bit(1) DEFAULT NULL,
  `console` longtext,
  PRIMARY KEY (`id`),
  KEY `fk_activity_instance_activity_idx` (`activity`),
  CONSTRAINT `fk_activity_instance_activity` FOREIGN KEY (`activity`) REFERENCES `activity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2322508 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_instance_parameter`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_instance_parameter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activity_instance` bigint(20) NOT NULL,
  `data_key` varchar(255) DEFAULT NULL,
  `data_value` longtext,
  PRIMARY KEY (`id`),
  KEY `activity_instance` (`activity_instance`),
  CONSTRAINT `activity_instance_parameter_ibfk_1` FOREIGN KEY (`activity_instance`) REFERENCES `activity_instance` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2214744 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_instance_persistent_data`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_instance_persistent_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activity_instance` bigint(20) NOT NULL,
  `data_key` varchar(255) DEFAULT NULL,
  `data_value` longtext,
  PRIMARY KEY (`id`),
  KEY `activity_instance` (`activity_instance`),
  CONSTRAINT `activity_instance_persistent_data_ibfk_1` FOREIGN KEY (`activity_instance`) REFERENCES `activity_instance` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4809434 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_queue`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_queue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `queue_opened` datetime DEFAULT NULL,
  `activity` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activity_queue_activity_instance`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity_queue_activity_instance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `queued` datetime DEFAULT NULL,
  `activity_instance` bigint(20) DEFAULT NULL,
  `activity_queue` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `asset_pricing_time_range`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `asset_pricing_time_range` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `offset_min` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(2048) DEFAULT NULL,
  `locator` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `generic_data_store`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `generic_data_store` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data_key` varchar(512) DEFAULT NULL,
  `data_value` longtext,
  PRIMARY KEY (`id`),
  KEY `data_key` (`data_key`(255))
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `importable_field`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `importable_field` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `log_message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date` datetime DEFAULT NULL,
  `message` longtext,
  `tag` varchar(255) DEFAULT NULL,
  `stack_trace` longtext,
  PRIMARY KEY (`id`),
  KEY `logMessage_tag` (`tag`),
  KEY `logMessage_date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=10920698 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `name_value_pair`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `name_value_pair` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource` bigint(20) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `resource` (`resource`),
  CONSTRAINT `name_value_pair_ibfk_3` FOREIGN KEY (`resource`) REFERENCES `resource` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(2048) DEFAULT NULL,
  `note_category` bigint(20) DEFAULT NULL,
  `content` longtext,
  `created` datetime DEFAULT NULL,
  `last_updated` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_note_note_category_idx` (`note_category`),
  CONSTRAINT `fk_note_note_category` FOREIGN KEY (`note_category`) REFERENCES `note_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_activity`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note` bigint(20) DEFAULT NULL,
  `activity` bigint(20) DEFAULT NULL,
  `main_note` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_note_activity_note_idx` (`note`),
  KEY `fk_note_activity_activity_idx` (`activity`),
  CONSTRAINT `fk_note_activity_activity` FOREIGN KEY (`activity`) REFERENCES `activity` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_note_activity_note` FOREIGN KEY (`note`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_file`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `file_name` varchar(2048) DEFAULT NULL,
  `content_type` varchar(2048) DEFAULT NULL,
  `note` bigint(20) DEFAULT NULL,
  `file_content` longblob,
  `comment` longtext,
  PRIMARY KEY (`id`),
  KEY `fk_note_file_note_idx` (`note`),
  CONSTRAINT `fk_note_file_note` FOREIGN KEY (`note`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_note`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_note` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note` bigint(20) DEFAULT NULL,
  `related_note` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_note_note_idx` (`note`),
  CONSTRAINT `fk_note_note` FOREIGN KEY (`note`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_publishing_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_publishing_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note` bigint(20) DEFAULT NULL,
  `publishing_configuration` bigint(20) DEFAULT NULL,
  `main_note` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_note_publishingconf_pub_idx` (`publishing_configuration`),
  KEY `fk_note_publishing_conf_note_idx` (`note`),
  CONSTRAINT `fk_note_publishingconf_pub` FOREIGN KEY (`publishing_configuration`) REFERENCES `publishing_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_note_publishing_conf_note` FOREIGN KEY (`note`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_resource_loader_plugin_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_resource_loader_plugin_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note` bigint(20) DEFAULT NULL,
  `resource_loader_plugin_configuration` bigint(20) DEFAULT NULL,
  `main_note` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_note_rlpc_rlpc_idx` (`resource_loader_plugin_configuration`),
  KEY `fk_note_rlpc_note_idx` (`note`),
  CONSTRAINT `fk_note_rlpc_note` FOREIGN KEY (`note`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_note_rlpc_rlpc` FOREIGN KEY (`resource_loader_plugin_configuration`) REFERENCES `resource_loader_plugin_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_spidering_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_spidering_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note` bigint(20) DEFAULT NULL,
  `spidering_configuration` bigint(20) DEFAULT NULL,
  `main_note` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_note_spidering_spidering_idx` (`spidering_configuration`),
  KEY `fk_note_spidering_note_idx` (`note`),
  CONSTRAINT `fk_note_spidering_note` FOREIGN KEY (`note`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_note_spidering_spidering` FOREIGN KEY (`spidering_configuration`) REFERENCES `spidering_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_time_series`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_time_series` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note` bigint(20) DEFAULT NULL,
  `time_series` bigint(20) DEFAULT NULL,
  `main_note` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `paging_url_fragment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `paging_url_fragment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `url_fragment_finder` bigint(20) DEFAULT NULL,
  `resource_configuration` bigint(20) NOT NULL,
  `fragment_query` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `url_fragment_finder` (`url_fragment_finder`),
  KEY `resource_configuration` (`resource_configuration`),
  CONSTRAINT `paging_url_fragment_ibfk_1` FOREIGN KEY (`url_fragment_finder`) REFERENCES `url_fragment_finder` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `paging_url_fragment_ibfk_2` FOREIGN KEY (`resource_configuration`) REFERENCES `resource_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publishing_category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `publishing_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publishing_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `publishing_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `publishing_strategy` longtext,
  `name` varchar(255) DEFAULT NULL,
  `publishing_category` bigint(20) DEFAULT NULL,
  `console` longtext,
  `uri` varchar(2048) DEFAULT NULL,
  `cache_data` longblob,
  `content_type` varchar(2048) DEFAULT NULL,
  `embed_in_layout` bit(1) DEFAULT NULL,
  `publishing_visibility` varchar(256) DEFAULT NULL,
  `locked_for_cache_update` bit(1) DEFAULT NULL,
  `locked_for_editing` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_publishing_configuration_cat_idx` (`publishing_category`),
  CONSTRAINT `fk_publishing_configuration_cat` FOREIGN KEY (`publishing_category`) REFERENCES `publishing_category` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publishing_configuration_cache_data`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `publishing_configuration_cache_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `publishing_configuration` bigint(20) DEFAULT NULL,
  `uri_parameter` varchar(2048) DEFAULT NULL,
  `cache_data` longblob,
  `content_type` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pub_cache_pub_idx` (`publishing_configuration`),
  CONSTRAINT `fk_pub_cache_pub` FOREIGN KEY (`publishing_configuration`) REFERENCES `publishing_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=84 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publishing_resource`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `publishing_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(2048) DEFAULT NULL,
  `publishing_configuration` bigint(20) DEFAULT NULL,
  `uri` varchar(2048) DEFAULT NULL,
  `publishing_strategy` longtext,
  `content_type` varchar(2048) DEFAULT NULL,
  `embed_in_layout` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pub_res_pub_idx` (`publishing_configuration`),
  CONSTRAINT `fk_pub_res_pub` FOREIGN KEY (`publishing_configuration`) REFERENCES `publishing_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publishing_resource_cache_data`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `publishing_resource_cache_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `publishing_resource` bigint(20) DEFAULT NULL,
  `uri_parameter` varchar(2048) DEFAULT NULL,
  `cache_data` longblob,
  `content_type` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_publ_resource_cache_publ_res_idx` (`publishing_resource`),
  CONSTRAINT `fk_publ_resource_cache_publ_res` FOREIGN KEY (`publishing_resource`) REFERENCES `publishing_resource` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8033 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `publishing_shared_data`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `publishing_shared_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `publishing_configuration` bigint(20) DEFAULT NULL,
  `data_key` varchar(2048) DEFAULT NULL,
  `text_data` longtext,
  `raw_data` longblob,
  `content_type` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_publ_shared_data_publ_conf_idx` (`publishing_configuration`),
  CONSTRAINT `fk_publ_shared_data_publ_conf` FOREIGN KEY (`publishing_configuration`) REFERENCES `publishing_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=133 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `required_activity`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `required_activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activity` bigint(20) NOT NULL,
  `required_activity` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `spidering_configuration` bigint(20) DEFAULT NULL,
  `base_level` bit(1) DEFAULT NULL,
  `depth` int(5) DEFAULT NULL,
  `paging` bit(1) DEFAULT NULL,
  `resource_loader_plugin_configuration` bigint(20) DEFAULT NULL,
  `paging_resource_loader_plugin_configuration` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_resource_conf_spidering_conf_idx` (`spidering_configuration`),
  KEY `fk_resource_conf_resource_load_a_idx` (`resource_loader_plugin_configuration`),
  KEY `fk_resource_conf_resource_load_b_idx` (`paging_resource_loader_plugin_configuration`),
  CONSTRAINT `fk_resource_conf_resource_load_a` FOREIGN KEY (`resource_loader_plugin_configuration`) REFERENCES `resource_loader_plugin_configuration` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_conf_resource_load_b` FOREIGN KEY (`paging_resource_loader_plugin_configuration`) REFERENCES `resource_loader_plugin_configuration` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_conf_spidering_conf` FOREIGN KEY (`spidering_configuration`) REFERENCES `spidering_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_loader_plugin_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_loader_plugin_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) DEFAULT NULL,
  `groovy_code` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `response_header`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `response_header` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource` bigint(20) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `resource` (`resource`),
  CONSTRAINT `response_header_ibfk_2` FOREIGN KEY (`resource`) REFERENCES `resource` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `base_level_resource` bigint(20) DEFAULT NULL,
  `first_found` datetime DEFAULT NULL,
  `last_found` datetime DEFAULT NULL,
  `spidering` bigint(20) DEFAULT NULL,
  `result_hash` varchar(255) DEFAULT NULL,
  `spidering_configuration` bigint(20) DEFAULT NULL,
  `is_valid` bit(1) DEFAULT NULL,
  `updated` bit(1) DEFAULT NULL,
  `site_identifier_hash` varchar(255) DEFAULT NULL,
  `uri_string` varchar(2048) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `result_hash` (`result_hash`),
  KEY `spidering_configuration` (`spidering_configuration`),
  KEY `spidering` (`spidering`),
  KEY `base_level_resource` (`base_level_resource`),
  KEY `site_identifier_hash` (`site_identifier_hash`),
  CONSTRAINT `result_ibfk_1` FOREIGN KEY (`spidering`) REFERENCES `spidering` (`id`),
  CONSTRAINT `result_ibfk_2` FOREIGN KEY (`spidering_configuration`) REFERENCES `spidering_configuration` (`id`),
  CONSTRAINT `result_ibfk_3` FOREIGN KEY (`spidering`) REFERENCES `spidering` (`id`),
  CONSTRAINT `result_ibfk_4` FOREIGN KEY (`base_level_resource`) REFERENCES `resource` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5474150 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result_unit`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result_unit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `result_unit_type` bigint(20) DEFAULT NULL,
  `result` bigint(20) DEFAULT NULL,
  `value` longtext CHARACTER SET utf8mb4,
  `site_identifier` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `result` (`result`),
  KEY `result_unit_type` (`result_unit_type`),
  CONSTRAINT `result_unit_ibfk_1` FOREIGN KEY (`result`) REFERENCES `result` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `result_unit_ibfk_2` FOREIGN KEY (`result_unit_type`) REFERENCES `result_unit_type` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34631628 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result_unit_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result_unit_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `result_unit_type` bigint(20) DEFAULT NULL,
  `result_unit_finder` bigint(20) DEFAULT NULL,
  `resource_configuration` bigint(20) NOT NULL,
  `finder_query` varchar(255) DEFAULT NULL,
  `site_identifier` bit(1) DEFAULT NULL,
  `locked_for_updates` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `result_unit_type` (`result_unit_type`),
  KEY `result_unit_finder` (`result_unit_finder`),
  KEY `resource_configuration` (`resource_configuration`),
  CONSTRAINT `result_unit_configuration_ibfk_1` FOREIGN KEY (`result_unit_type`) REFERENCES `result_unit_type` (`id`),
  CONSTRAINT `result_unit_configuration_ibfk_2` FOREIGN KEY (`result_unit_finder`) REFERENCES `result_unit_finder` (`id`),
  CONSTRAINT `result_unit_configuration_ibfk_3` FOREIGN KEY (`resource_configuration`) REFERENCES `resource_configuration` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result_unit_configuration_modifiers`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result_unit_configuration_modifiers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `result_unit_configuration` bigint(20) DEFAULT NULL,
  `result_unit_modifier_configuration` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rucm_ruc_idx` (`result_unit_configuration`),
  KEY `fk_rucm_rumc_idx` (`result_unit_modifier_configuration`),
  CONSTRAINT `fk_rucm_ruc` FOREIGN KEY (`result_unit_configuration`) REFERENCES `result_unit_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_rucm_rumc` FOREIGN KEY (`result_unit_modifier_configuration`) REFERENCES `result_unit_modifier_configuration` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result_unit_finder`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result_unit_finder` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result_unit_modifier_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result_unit_modifier_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) DEFAULT NULL,
  `groovy_code` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result_unit_type`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result_unit_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `result_verifier_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `result_verifier_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) DEFAULT NULL,
  `groovy_code` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spidering`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spidering` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `finished` datetime DEFAULT NULL,
  `started` datetime DEFAULT NULL,
  `spidering_configuration` bigint(20) DEFAULT NULL,
  `result_count` int(10) DEFAULT NULL,
  `valid_result_count` int(10) DEFAULT NULL,
  `new_result_count` int(10) DEFAULT NULL,
  `updated_result_count` int(10) DEFAULT NULL,
  `unchanged_result_count` int(10) DEFAULT NULL,
  `invalid_result_count` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_spidering_spidering_conf_idx` (`spidering_configuration`),
  CONSTRAINT `fk_spidering_spidering_conf` FOREIGN KEY (`spidering_configuration`) REFERENCES `spidering_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2208158 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spidering_configuration`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spidering_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `cron_schedule` varchar(512) DEFAULT NULL,
  `cron_schedule_enabled` bit(1) DEFAULT NULL,
  `result_verifier_configuration` bigint(20) DEFAULT NULL,
  `check_duplicates_globally` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_spidering_configuration_verifier_idx` (`result_verifier_configuration`),
  CONSTRAINT `fk_spidering_configuration_verifier` FOREIGN KEY (`result_verifier_configuration`) REFERENCES `result_verifier_configuration` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spidering_post_activity`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spidering_post_activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activity` bigint(20) DEFAULT NULL,
  `spidering_configuration` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_spidering_post_spidering_conf_idx` (`spidering_configuration`),
  KEY `fk_spidering_post_activity_idx` (`activity`),
  CONSTRAINT `fk_spidering_post_activity` FOREIGN KEY (`activity`) REFERENCES `activity` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_spidering_post_spidering_conf` FOREIGN KEY (`spidering_configuration`) REFERENCES `spidering_configuration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `time_series`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `time_series` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) DEFAULT NULL,
  `time_series_type` bigint(20) DEFAULT NULL,
  `indexable` bit(1) DEFAULT NULL,
  `locator` varchar(2048) DEFAULT NULL,
  `source` varchar(512) DEFAULT NULL,
  `source_id` varchar(512) DEFAULT NULL,
  `approximate_size` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `name` (`name`(255)),
  KEY `locator` (`locator`(255)),
  FULLTEXT KEY `name_2` (`name`)
) ENGINE=MyISAM AUTO_INCREMENT=400640 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `time_series_type`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `time_series_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(512) DEFAULT NULL,
  `observation_view` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trigger_activity`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trigger_activity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `activity` bigint(20) NOT NULL,
  `trigger_activity` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `url_fragment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `url_fragment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `url_fragment_finder` bigint(20) DEFAULT NULL,
  `resource_configuration` bigint(20) NOT NULL,
  `fragment_query` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `url_fragment_finder` (`url_fragment_finder`),
  KEY `resource_configuration` (`resource_configuration`),
  CONSTRAINT `url_fragment_ibfk_1` FOREIGN KEY (`url_fragment_finder`) REFERENCES `url_fragment_finder` (`id`),
  CONSTRAINT `url_fragment_ibfk_2` FOREIGN KEY (`resource_configuration`) REFERENCES `resource_configuration` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `url_fragment_finder`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `url_fragment_finder` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_roles` (
  `user` bigint(20) NOT NULL,
  `roles` bigint(20) NOT NULL,
  PRIMARY KEY (`user`,`roles`),
  KEY `FK73429949F592E2E1` (`roles`),
  KEY `FK73429949EF50AB44` (`user`),
  CONSTRAINT `FK73429949EF50AB44` FOREIGN KEY (`user`) REFERENCES `user` (`id`),
  CONSTRAINT `FK73429949F592E2E1` FOREIGN KEY (`roles`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-09-10 16:53:44

CREATE TABLE IF NOT EXISTS `resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `url` varchar(2048) DEFAULT NULL,
  `content` longtext,
  `raw_content` longblob,
  `mime_type` varchar(1024) DEFAULT NULL,
  `http_method` varchar(512) DEFAULT NULL,
  `load_succeed` bit(1) DEFAULT NULL,
  `is_binary` bit(1) DEFAULT NULL,
  `http_status_code` int(5) DEFAULT NULL,
  `encoding` varchar(512) DEFAULT NULL,
  `depth` int(5) DEFAULT NULL,
  `previous_resource` bigint(20) DEFAULT NULL,
  `paging_depth` int(5) DEFAULT NULL,
  `spidering` bigint(20) DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `previous_resource` (`previous_resource`),
  KEY `spidering` (`spidering`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;

--
-- Constraints der exportierten Tabellen
--

--
-- Constraints der Tabelle `resource`
--
ALTER TABLE `resource`
  ADD CONSTRAINT `resource_ibfk_4` FOREIGN KEY (`spidering`) REFERENCES `spidering` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
