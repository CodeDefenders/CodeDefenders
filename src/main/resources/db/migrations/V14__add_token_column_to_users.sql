/* Add "Token" column to users. */
ALTER TABLE users
    ADD `Token` char(32) DEFAULT NULL;
