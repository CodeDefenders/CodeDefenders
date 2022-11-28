/* Resize "Username" column to 46 (20+1+20+1+4) chars to support external users. */
ALTER TABLE users
    MODIFY Username VARCHAR(46);