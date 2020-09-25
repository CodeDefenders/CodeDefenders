/* Add "Active" column to puzzles. */
ALTER TABLE puzzles
    ADD `Active` tinyint(1) DEFAULT '1';
