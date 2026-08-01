CREATE DATABASE IF NOT EXISTS `mifosplatform-tenants` CHARACTER SET utf8 COLLATE utf8_general_ci;
CREATE DATABASE IF NOT EXISTS `mifostenant-default` CHARACTER SET utf8 COLLATE utf8_general_ci;

GRANT ALL PRIVILEGES ON `mifosplatform-tenants`.* TO 'mifos'@'%';
GRANT ALL PRIVILEGES ON `mifostenant-default`.* TO 'mifos'@'%';
GRANT ALL PRIVILEGES ON `mifostenant-%`.* TO 'mifos'@'%';
FLUSH PRIVILEGES;
