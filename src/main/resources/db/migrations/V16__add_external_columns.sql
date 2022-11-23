/* Add "External" column to users and games. */
ALTER TABLE games
    ADD `External` VARCHAR(100) NULL DEFAULT NULL;
ALTER TABLE users
    ADD `External` tinyint(1) DEFAULT 0;