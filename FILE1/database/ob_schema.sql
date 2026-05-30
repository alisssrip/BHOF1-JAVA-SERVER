-- ---------------------------------------------------------------------------
-- Reference schema for the Outbreak matchmaking/bypass backend (ob_* tables)
--
-- These are the tables the Java servers expect a backend to be built around.
-- This is a reference, not a requirement: your backend may model the data any
-- way it likes, as long as it honours the API contract documented in README.md.
--
-- Engine/charset are kept as in the reference deployment (InnoDB / latin1).
-- ---------------------------------------------------------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- ob_gameservers
-- Registered game (bypass) servers. One row per owner. Written by the
-- gameservers/register|heartbeat|unregister endpoints. `public_ip` should be
-- set by the backend from the connection, not trusted from the request body.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ob_gameservers`;
CREATE TABLE `ob_gameservers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_userid` varchar(14) NOT NULL,
  `public_ip` varchar(45) DEFAULT NULL,
  `port` int(11) DEFAULT 8690,
  `region` varchar(10) DEFAULT NULL,
  `verified` tinyint(1) DEFAULT 0,
  `last_heartbeat` timestamp NULL DEFAULT current_timestamp(),
  `registered_at` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_owner` (`owner_userid`),
  KEY `idx_verified` (`verified`),
  KEY `idx_heartbeat` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- ob_hnpairs
-- Handle / nickname pairs per user. Read via users/{userid}/hnpairs and
-- handles/{handle}; written via the hnpairs endpoints.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ob_hnpairs`;
CREATE TABLE `ob_hnpairs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `handle` varchar(6) DEFAULT NULL,
  `userid` varchar(14) DEFAULT NULL,
  `nickname` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `handle` (`handle`),
  KEY `userid` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- ob_motd
-- Message of the day. Served by server/motd (the active row).
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ob_motd`;
CREATE TABLE `ob_motd` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `message` varchar(2000) DEFAULT NULL,
  `active` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- ob_sessions
-- Active player sessions. Created by the web/login flow, updated by the
-- matchmaking (origin/game/online) and wiped by sessions/reset on startup.
-- `gamesess` is an internal Outbreak game number, not a server reference.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ob_sessions`;
CREATE TABLE `ob_sessions` (
  `sessid` varchar(8) NOT NULL,
  `userid` varchar(50) DEFAULT NULL,
  `ip` varchar(15) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `region` varchar(8) DEFAULT NULL,
  `gamesess` bigint(20) DEFAULT NULL,
  `area` int(11) DEFAULT -1,
  `room` int(11) DEFAULT 0,
  `slot` int(11) DEFAULT 0,
  `state` int(11) DEFAULT 0,
  `online` tinyint(1) NOT NULL DEFAULT 0,
  `lastlogin` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`sessid`),
  KEY `gamesess` (`gamesess`),
  KEY `idx_online` (`online`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- ob_users
-- Minimal user mapping (userid -> numeric id). The full user/account data
-- lives in the backend's own user store; this table is the bridge the Java
-- servers reference by userid.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ob_users`;
CREATE TABLE `ob_users` (
  `userid` varchar(50) NOT NULL,
  `id` int(11) DEFAULT NULL,
  PRIMARY KEY (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ---------------------------------------------------------------------------
-- ob_servers
-- Regional game servers you operate, one row per region (or more). The
-- matchmaking looks a server up here by the session's region (servers_api +
-- byuser/{userid}). You MUST have at least one active row whose `region`
-- matches the region assigned to a player's session, otherwise the lookup
-- fails and the player is routed to the matchmaking server itself as a
-- fallback game server. `active` must be 1 for the row to be used.
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ob_servers`;
CREATE TABLE `ob_servers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `region` varchar(8) NOT NULL,
  `ip` varchar(45) NOT NULL,
  `port` int(11) NOT NULL DEFAULT 8690,
  `name` varchar(64) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_region` (`region`),
  KEY `idx_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

SET FOREIGN_KEY_CHECKS = 1;
