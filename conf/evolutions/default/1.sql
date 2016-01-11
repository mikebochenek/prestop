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
ENGINE = InnoDB

exit;

