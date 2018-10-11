DROP TABLE IF EXISTS puzzles;
DROP TABLE IF EXISTS puzzle_levels;

CREATE TABLE puzzle_levels
(
    Level_ID INTEGER NOT NULL AUTO_INCREMENT,
    Position INTEGER DEFAULT NULL, -- Index of the level in the levels list
    Name VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    CONSTRAINT puzzle_levels_pk PRIMARY KEY (Level_ID)
);

CREATE TABLE puzzles
(
    Puzzle_ID INTEGER NOT NULL AUTO_INCREMENT,
    Class_ID INTEGER NOT NULL,
    Active_Role ENUM('ATTACKER', 'DEFENDER') NOT NULL,
    Level_ID INTEGER DEFAULT NULL, -- Level (group of puzzles) this puzzle belongs to
    Position INTEGER DEFAULT NULL, -- Index of the puzzle in the level
    Title VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    Editable_Lines_Start INTEGER DEFAULT NULL,
    Editable_Lines_End INTEGER DEFAULT NULL,
    Difficulty ENUM('EASY', 'HARD') DEFAULT 'HARD',
    CONSTRAINT puzzles_pk PRIMARY KEY (Puzzle_ID),
    CONSTRAINT puzzles_classes_Class_ID_fk FOREIGN KEY (Class_ID) REFERENCES classes (Class_ID) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT puzzles_puzzle_level_Level_fk FOREIGN KEY (Level_ID) REFERENCES puzzle_levels (Level_ID) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT puzzles_level_index_unique UNIQUE (Level_ID, Position)
);

ALTER TABLE classes ADD Puzzle TINYINT(1) DEFAULT 0 NOT NULL;
