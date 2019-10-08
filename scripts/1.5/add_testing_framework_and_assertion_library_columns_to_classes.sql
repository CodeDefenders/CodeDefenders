/* Add "TestingFramework" and "AssertionLibrary" columns to classes. */
ALTER TABLE classes ADD `TestingFramework`
    ENUM('JUNIT4', 'JUNIT5')
    NOT NULL DEFAULT 'JUNIT4';

ALTER TABLE classes ADD `AssertionLibrary`
    ENUM('JUNIT4', 'JUNIT5', 'HAMCREST', 'JUNIT4_HAMCREST', 'JUNIT5_HAMCREST')
    NOT NULL DEFAULT 'JUNIT4_HAMCREST';

/* Update classes views */
CREATE OR REPLACE VIEW `view_active_classes` AS
SELECT *
FROM classes
WHERE Active = 1;

CREATE OR REPLACE VIEW `view_playable_classes` AS
SELECT *
FROM `view_active_classes`
WHERE Puzzle = 0;

CREATE OR REPLACE VIEW `view_puzzle_classes` AS
SELECT *
FROM `view_active_classes`
WHERE Puzzle = 1;
