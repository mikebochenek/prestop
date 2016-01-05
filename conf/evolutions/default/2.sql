
CREATE TABLE `tag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner` int(11) DEFAULT NULL,
  `tag` varchar(255) DEFAULT NULL,
  `createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ;

CREATE TABLE `donetag` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tagid` int(11) DEFAULT NULL,
  `doneid` int(11) DEFAULT NULL,
  `createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ;

CREATE TABLE `team` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner` int(11) DEFAULT NULL,
  `target` int(11) DEFAULT NULL,
  `createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT NULL,
  `status` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ;

CREATE TABLE `maillog` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `recipient` int(11) DEFAULT NULL,
  `subject` text,
  `content` text,
  `createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ;


ALTER TABLE `tag` ADD INDEX `tag_owner_idx` (`owner`);

ALTER TABLE `donetag` ADD INDEX `donetag_tagid_idx` (`tagid`);
ALTER TABLE `donetag` ADD INDEX `donetag_doneid_idx` (`doneid`);

ALTER TABLE `team` ADD INDEX `team_owner_idx` (`owner`);
ALTER TABLE `team` ADD INDEX `team_target_idx` (`target`);

ALTER TABLE `maillog` ADD INDEX `maillog_recipient_idx` (`recipient`);
ALTER TABLE `maillog` ADD INDEX `maillog_status_idx` (`status`);

