mysql -u root -p

create database presto;
create user prestouser;
grant all privileges on presto.* to prestouser@"localhost" identified by 'password';  
exit;

mysql -u prestouser -p

use presto;  

CREATE TABLE IF NOT EXISTS `presto`.`user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `createdate` DATETIME NULL,
  `lastlogindate` TIMESTAMP NULL,
  `deleted` BIT NULL,
  `password` VARCHAR(255) NULL,
  `settings` LONGTEXT NULL,
  `username` VARCHAR(45) NULL,
  `email` VARCHAR(45) NULL,
  `type` VARCHAR(45) NULL,
  `openidtoken` VARCHAR(45) NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `presto`.`restaurant` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `city` VARCHAR(45) NULL,
  `address` VARCHAR(255) NULL,
  `longitude` DOUBLE NULL,
  `latitude` DOUBLE NULL,
  `schedulecron` VARCHAR(45) NULL,
  `restype` INT NULL,
  `lastupdate` TIMESTAMP NULL,
  `status` INT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `presto`.`dish` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `price` DOUBLE NULL,
  `name` VARCHAR(255) NULL,
  `vegetarian` INT NULL,
  `gluton` INT NULL,
  `diary` INT NULL,
  `greenscore` DOUBLE NULL,
  `lastupdate` TIMESTAMP NULL,
  `status` INT NULL,
  `restaurant_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_dish_restaurant_idx` (`restaurant_id` ASC),
  CONSTRAINT `fk_dish_restaurant`
    FOREIGN KEY (`restaurant_id`)
    REFERENCES `presto`.`restaurant` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


/* thats weird, some of my changes are not there... and version history seems to be missing */

CREATE TABLE IF NOT EXISTS `presto`.`image` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `filename` VARCHAR(255) NULL,
  `url` VARCHAR(255) NULL,
  `restaurant_id` INT NOT NULL,
  `dish_id` INT NOT NULL,
  `status` INT NULL,
  `lastupdate` TIMESTAMP NULL,
  `user_id` INT NULL,
  `width` INT NULL,
  `height` INT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


ALTER TABLE `presto`.`image`  ADD `user_id` INT NULL;
ALTER TABLE `presto`.`image`  ADD `width` INT NULL;
ALTER TABLE `presto`.`image`  ADD `height` INT NULL;


CREATE TABLE IF NOT EXISTS `presto`.`reservation` (
  `user_id` INT NOT NULL,
  `restaurant_id` INT NOT NULL,
  `id` INT NOT NULL AUTO_INCREMENT,
  `reservationtime` TIMESTAMP NULL,
  `guestcount` INT NULL,
  `special_requests` VARCHAR(255) NULL,
  `status` INT NULL,
  `source` INT NULL,
  `source_id` VARCHAR(128) NULL,
  `lastupdate` TIMESTAMP NULL,
  PRIMARY KEY (`id`, `user_id`, `restaurant_id`),
  INDEX `fk_user_has_restaurant_restaurant1_idx` (`restaurant_id` ASC),
  INDEX `fk_user_has_restaurant_user1_idx` (`user_id` ASC),
  CONSTRAINT `fk_user_has_restaurant_user1`
    FOREIGN KEY (`user_id`)
    REFERENCES `presto`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_user_has_restaurant_restaurant1`
    FOREIGN KEY (`restaurant_id`)
    REFERENCES `presto`.`restaurant` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `presto`.`restaurant_seating` (
  `tables` INT NOT NULL,
  `restaurant_id` INT NOT NULL,
  `reservation_id` INT,
  `day` DATE,
  `id` INT NOT NULL AUTO_INCREMENT,
  `lastupdate` TIMESTAMP NULL,
  `misc` VARCHAR(8192) NULL,
  PRIMARY KEY (`id`, `restaurant_id`),
  INDEX `fk_rest_seating_reservation_idx` (`reservation_id` ASC),
  INDEX `fk_rest_seating_restaurant1_idx` (`restaurant_id` ASC))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `presto`.`activity_log` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `createdate` TIMESTAMP NULL,
  `activity_type` INT NULL,
  `activity_subtype` INT NULL,
  `activity_details` TEXT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_activity_log_user1_idx` (`user_id` ASC),
  CONSTRAINT `fk_activity_log_user1`
    FOREIGN KEY (`user_id`)
    REFERENCES `presto`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `presto`.`friend` (
  `user_id` INT NOT NULL,
  `friend_user_id` INT NOT NULL,
  `id` INT NOT NULL AUTO_INCREMENT,
  `status` INT NULL,
  `lastupdate` TIMESTAMP NULL,
  PRIMARY KEY (`id`, `user_id`, `friend_user_id`),
  INDEX `fk_user_has_user_user2_idx` (`friend_user_id` ASC),
  INDEX `fk_user_has_user_user1_idx` (`user_id` ASC),
  CONSTRAINT `fk_user_has_user_user1`
    FOREIGN KEY (`user_id`)
    REFERENCES `presto`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_user_has_user_user2`
    FOREIGN KEY (`friend_user_id`)
    REFERENCES `presto`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `presto`.`tagref` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `tagid` INT NOT NULL,
  `refid` INT NOT NULL,
  `status` INT NULL,
  `lastupdate` TIMESTAMP NULL,
  PRIMARY KEY (`id`, `tagid`, `refid`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `presto`.`tag` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL,
  `en_text` VARCHAR(255) NULL,
  `de_text` VARCHAR(255) NULL,
  `it_text` VARCHAR(255) NULL,
  `fr_text` VARCHAR(255) NULL,
  `status` INT NULL,
  `lastupdate` TIMESTAMP NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;

ALTER TABLE `presto`.`restaurant` ADD `email` VARCHAR( 45 );
ALTER TABLE `presto`.`restaurant` ADD `phone` VARCHAR( 45 );

CREATE TABLE IF NOT EXISTS `presto`.`maillog` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `recipient` int(11) DEFAULT NULL,
  `subject` text,
  `content` text,
  `createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `maillog_recipient_idx` (`recipient`),
  KEY `maillog_status_idx` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

ALTER TABLE `presto`.`restaurant` ADD `postalcode` VARCHAR( 45 );
ALTER TABLE `presto`.`restaurant` ADD `state` VARCHAR( 45 );

ALTER TABLE `presto`.`dish` DROP COLUMN gluton;
ALTER TABLE `presto`.`dish` DROP COLUMN diary;
ALTER TABLE `presto`.`dish` DROP COLUMN vegetarian;

/* update user set type = 7; */


/* 28.02.2016 - updates for making schedule field longer! */
ALTER TABLE `presto`.`restaurant`  
  CHANGE COLUMN `schedulecron` `schedulecron` VARCHAR(1024);

/* 05.03.2016 - add more fields to users table */
ALTER TABLE `presto`.`user`  ADD `fullname` VARCHAR(45) NULL;
ALTER TABLE `presto`.`user`  ADD `city` VARCHAR(45) NULL;
ALTER TABLE `presto`.`user`  ADD `country` VARCHAR(25) NULL;
ALTER TABLE `presto`.`user`  ADD `state` VARCHAR(25) NULL;
ALTER TABLE `presto`.`user`  ADD `phone` VARCHAR(35) NULL;

update user set city = 'Zurich', country = 'Switzerland' where id = 1 or id = 2;
update user set fullname = 'Mike Bochenek' where id = 1;


/* uh... oh, what happened to my alter statements for restaurant - such as state etc */


CREATE TABLE IF NOT EXISTS `presto`.`restaurant_owner` (
  `id` INT NOT NULL,
  `restaurant_id` INT NOT NULL,
  `user_id` INT NOT NULL,
  `status` INT NULL,
  `lastupdate` TIMESTAMP NULL,
  `settings` VARCHAR(2000) NULL,
  PRIMARY KEY (`id`, `user_id`, `restaurant_id`))
ENGINE = InnoDB;

/* 12.04.2016 - adding description and serving size to dish, and adding multiple locations */
ALTER TABLE `presto`.`dish`  ADD `serving` VARCHAR(255) NULL;
ALTER TABLE `presto`.`dish`  ADD `description` VARCHAR(2048) NULL;

ALTER TABLE `presto`.`restaurant`  ADD `parent_id` INT NULL;
ALTER TABLE `presto`.`restaurant`  ADD `website` VARCHAR(255) NULL;

ALTER TABLE `presto`.`restaurant`  ADD `country` VARCHAR(65) NULL;

/* 24.05.2016 - adding dish source */
ALTER TABLE `presto`.`dish`  ADD `source` VARCHAR(512) NULL;

/* 02.06.2016 - fixing friends limitation */
ALTER TABLE friend DROP FOREIGN KEY fk_user_has_user_user2;
alter table friend modify friend_user_id bigint;
alter table friend add index (friend_user_id);

/* 03.06.2016 - adding misc which can contain country and state etc. */
ALTER TABLE `presto`.`restaurant` MODIFY  `state` VARCHAR(4096) NULL;

/* 18.06.2016 - adding restaurant google places ID */
ALTER TABLE `presto`.`restaurant`  ADD `google_places_id` VARCHAR(35) NULL;

/* 13.07.2016 - adding AUTO_INCREMENT to restaurant_owner */
ALTER TABLE restaurant_owner CHANGE id id INT AUTO_INCREMENT;

/* 02.12.2016 - adding price bucket to dish */
ALTER TABLE `presto`.`dish`  ADD `price_bucket` VARCHAR(55) NULL;

/* 21.05.2017 - adding index on activity_log */
CREATE INDEX idx_activity_log_all ON activity_log(user_id, createdate, activity_type);

show index from activity_log;

exit;

