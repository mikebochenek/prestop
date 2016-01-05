# --- First database schema

# --- !Ups

delimiter $$

CREATE TABLE `done` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner` int(11) DEFAULT NULL,
  `donetext` text,
  `donedate` datetime DEFAULT NULL,
  `createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT NULL,
  `category` bigint(20) NOT NULL,
  `doneDay` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ;

CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `createdate` datetime DEFAULT NULL,
  `lastlogindate` datetime DEFAULT NULL,
  `deleted` bit(1) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `settings` longtext,
  `username` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `openidtoken` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ;


ALTER TABLE `done` ADD INDEX `done_owner_idx` (`owner`);
ALTER TABLE `done` ADD INDEX `done_doneday_idx` (`doneday`);
ALTER TABLE `user` ADD INDEX `user_email_idx` (`email`);


INSERT INTO `test`.`user` (`id`, `password`, `username`, `email`) 
VALUES (1, 'test', 'mike', 'mike@test.com' );


# http://stackoverflow.com/questions/1071180/is-the-primary-key-automatically-indexed-in-mysql
# ALTER TABLE `done` ADD INDEX `done_id_idx` (`id`);
# ALTER TABLE `user` ADD INDEX `user_id_idx` (`id`);

#create table user (
#  email                     varchar(255) not null primary key,
#  name                      varchar(255) not null,
#  password                  varchar(255) not null
#);

#create table done (
#  id                        bigint not null primary key,
#  name                      varchar(255) not null,
#  folder                    varchar(255) not null
# );

# create sequence done_seq start with 1000;


# --- !Downs

drop table if exists done;
drop sequence if exists done_seq;
drop table if exists user;
