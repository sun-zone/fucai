-- MySQL dump 10.13  Distrib 8.3.0, for macos14 (arm64)
--
-- Host: localhost    Database: fucai
-- ------------------------------------------------------
-- Server version	8.3.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `dlt_record`
--

DROP TABLE IF EXISTS `dlt_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dlt_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expect` varchar(20) NOT NULL,
  `front1` varchar(2) NOT NULL,
  `front2` varchar(2) NOT NULL,
  `front3` varchar(2) NOT NULL,
  `front4` varchar(2) NOT NULL,
  `front5` varchar(2) NOT NULL,
  `back1` varchar(2) NOT NULL,
  `back2` varchar(2) NOT NULL,
  `created_at` datetime NOT NULL,
  `draw_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dlt_record_expect` (`expect`),
  UNIQUE KEY `UKta638umgvpj0xpsh1ntc2wdqs` (`expect`)
) ENGINE=InnoDB AUTO_INCREMENT=2930 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kl8_feature_weight`
--

DROP TABLE IF EXISTS `kl8_feature_weight`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kl8_feature_weight` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contribution` double DEFAULT NULL,
  `feature_name` varchar(60) NOT NULL,
  `hit_average` double DEFAULT NULL,
  `miss_average` double DEFAULT NULL,
  `weight` double DEFAULT NULL,
  `run_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kl8_feature_weight_run` (`run_id`),
  KEY `idx_kl8_feature_weight_name` (`feature_name`),
  CONSTRAINT `FKtg6v4rpr8vpld1to4fdrpq8bk` FOREIGN KEY (`run_id`) REFERENCES `kl8_weight_run` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=721 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kl8_rank_hit_stat`
--

DROP TABLE IF EXISTS `kl8_rank_hit_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kl8_rank_hit_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_expect` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `number` varchar(2) NOT NULL,
  `overall_rank` int NOT NULL,
  `target_expect` varchar(20) NOT NULL,
  `tier_name` varchar(20) NOT NULL,
  `tier_rank` int NOT NULL,
  `window_size` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfxybj4eoqo4y14t2u9mgtg11m` (`window_size`,`target_expect`,`number`),
  KEY `idx_kl8_rank_hit_window_expect` (`window_size`,`target_expect`),
  KEY `idx_kl8_rank_hit_overall_rank` (`window_size`,`overall_rank`),
  KEY `idx_kl8_rank_hit_tier_rank` (`window_size`,`tier_name`,`tier_rank`)
) ENGINE=InnoDB AUTO_INCREMENT=33882 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kl8_recommendation_backtest`
--

DROP TABLE IF EXISTS `kl8_recommendation_backtest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kl8_recommendation_backtest` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actual_numbers` longtext NOT NULL,
  `base_expect` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `hit_count` int NOT NULL,
  `hit_numbers` longtext NOT NULL,
  `hit_rate` double DEFAULT NULL,
  `predicted_count` int NOT NULL,
  `predicted_numbers` longtext NOT NULL,
  `target_expect` varchar(20) NOT NULL,
  `window_size` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKljxkyj6ve9610732p6n5d8swr` (`window_size`,`target_expect`),
  KEY `idx_kl8_backtest_window_expect` (`window_size`,`target_expect`),
  KEY `idx_kl8_backtest_hit_count` (`window_size`,`hit_count`)
) ENGINE=InnoDB AUTO_INCREMENT=501 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kl8_record`
--

DROP TABLE IF EXISTS `kl8_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kl8_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expect` varchar(20) NOT NULL,
  `num1` varchar(2) NOT NULL,
  `num2` varchar(2) NOT NULL,
  `num3` varchar(2) NOT NULL,
  `num4` varchar(2) NOT NULL,
  `num5` varchar(2) NOT NULL,
  `num6` varchar(2) NOT NULL,
  `num7` varchar(2) NOT NULL,
  `num8` varchar(2) NOT NULL,
  `num9` varchar(2) NOT NULL,
  `num10` varchar(2) NOT NULL,
  `num11` varchar(2) NOT NULL,
  `num12` varchar(2) NOT NULL,
  `num13` varchar(2) NOT NULL,
  `num14` varchar(2) NOT NULL,
  `num15` varchar(2) NOT NULL,
  `num16` varchar(2) NOT NULL,
  `num17` varchar(2) NOT NULL,
  `num18` varchar(2) NOT NULL,
  `num19` varchar(2) NOT NULL,
  `num20` varchar(2) NOT NULL,
  `created_at` datetime NOT NULL,
  `draw_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kl8_record_expect` (`expect`),
  UNIQUE KEY `UKq0bxa88ua68py3phtcbg4espd` (`expect`)
) ENGINE=InnoDB AUTO_INCREMENT=3403 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kl8_weight_run`
--

DROP TABLE IF EXISTS `kl8_weight_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kl8_weight_run` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `average_hit_count` double DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_expect` varchar(20) DEFAULT NULL,
  `hit_number_count` int NOT NULL,
  `sample_issue_count` int NOT NULL,
  `sample_number_count` int NOT NULL,
  `start_expect` varchar(20) DEFAULT NULL,
  `window_size` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kl8_weight_run_created` (`created_at`),
  KEY `idx_kl8_weight_run_window` (`window_size`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ssq_recommendation_backtest`
--

DROP TABLE IF EXISTS `ssq_recommendation_backtest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ssq_recommendation_backtest` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actual_blue` varchar(2) NOT NULL,
  `actual_red` varchar(40) NOT NULL,
  `average_red_hit_count` double DEFAULT NULL,
  `base_expect` varchar(20) NOT NULL,
  `best_red_hit_count` int NOT NULL,
  `blue_groups` longtext NOT NULL,
  `blue_hit_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `red_groups` longtext NOT NULL,
  `target_expect` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKsjd9asdtq9p7fauekxw6e460k` (`target_expect`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ssq_record`
--

DROP TABLE IF EXISTS `ssq_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ssq_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expect` varchar(20) NOT NULL,
  `red1` varchar(2) NOT NULL,
  `red2` varchar(2) NOT NULL,
  `red3` varchar(2) NOT NULL,
  `red4` varchar(2) NOT NULL,
  `red5` varchar(2) NOT NULL,
  `red6` varchar(2) NOT NULL,
  `blue` varchar(2) NOT NULL,
  `created_at` datetime NOT NULL,
  `draw_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expect` (`expect`),
  UNIQUE KEY `UKc76dqat11ow6h1bytnbcjxpmr` (`expect`)
) ENGINE=InnoDB AUTO_INCREMENT=3515 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'fucai'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-06 22:29:44
