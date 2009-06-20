CREATE TABLE `messages` (
  `id` int(11) NOT NULL auto_increment,
  `nick` varchar(64) default NULL,
  `message` text,
  `classification` varchar(16) default NULL,
  `date` timestamp NOT NULL default CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`),
  KEY `nick` (`nick`),
  KEY `classification` (`classification`),
  KEY `i_date` (`date`)
) ENGINE=MyISAM AUTO_INCREMENT=28880 DEFAULT CHARSET=latin1;
