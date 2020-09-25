/* Add "Active" column to puzzles. */
ALTER TABLE puzzles
    ADD `Active` tinyint(1) DEFAULT '1';

/* Add "Parent_Class" column to classes */
ALTER TABLE classes ADD `Parent_Class` int(11) DEFAULT NULL;
