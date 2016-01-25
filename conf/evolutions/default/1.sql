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


CREATE TABLE IF NOT EXISTS `presto`.`image` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `filename` VARCHAR(255) NULL,
  `url` VARCHAR(255) NULL,
  `restaurant_id` INT NOT NULL,
  `dish_id` INT NOT NULL,
  `status` INT NULL,
  `lastupdate` TIMESTAMP NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


CREATE TABLE IF NOT EXISTS `presto`.`reservation` (
  `user_id` INT NOT NULL,
  `restaurant_id` INT NOT NULL,
  `id` INT NOT NULL AUTO_INCREMENT,
  `reservationtime` TIMESTAMP NULL,
  `guestcount` INT NULL,
  `special_requests` VARCHAR(255) NULL,
  `status` INT NULL,
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

-- below statements are not deployed to digitalocean yet --

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


exit;

