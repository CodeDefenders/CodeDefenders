DROP TABLE IF EXISTS puzzles;
DROP TABLE IF EXISTS puzzle_chapters;

CREATE TABLE puzzle_chapters
(
    Chapter_ID INTEGER NOT NULL AUTO_INCREMENT,
    Position INTEGER DEFAULT NULL, -- Index of the chapter in the puzzles list
    Title VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    CONSTRAINT puzzle_chapters_pk PRIMARY KEY (Chapter_ID)
);

CREATE TABLE puzzles
(
    Puzzle_ID INTEGER NOT NULL AUTO_INCREMENT,
    Class_ID INTEGER NOT NULL,
    Active_Role ENUM('ATTACKER', 'DEFENDER') NOT NULL,
    Level ENUM('EASY', 'HARD') DEFAULT 'HARD',
    Editable_Lines_Start INTEGER DEFAULT NULL,
    Editable_Lines_End INTEGER DEFAULT NULL,
    Chapter_ID INTEGER DEFAULT NULL, -- Chapter (group of puzzles) this puzzle belongs to
    Position INTEGER DEFAULT NULL, -- Index of the puzzle in the chapter
    Title VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    CONSTRAINT puzzles_pk PRIMARY KEY (Puzzle_ID),
    CONSTRAINT puzzles_classes_Class_ID_fk FOREIGN KEY (Class_ID) REFERENCES classes (Class_ID) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT puzzles_puzzle_chapters_Level_fk FOREIGN KEY (Chapter_ID) REFERENCES puzzle_chapters (Chapter_ID) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT puzzles_level_index_unique UNIQUE (Chapter_ID, Position)
);

ALTER TABLE classes ADD Puzzle TINYINT(1) DEFAULT 0 NOT NULL;
