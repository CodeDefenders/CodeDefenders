DROP TABLE IF EXISTS puzzles;
DROP TABLE IF EXISTS puzzle_chapters;

CREATE TABLE puzzle_chapters
(
    Chapter_ID INTEGER NOT NULL AUTO_INCREMENT,
    Position INTEGER DEFAULT NULL, -- Index of the chapter in the puzzles list
    Title VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    CONSTRAINT puzzle_chapters_pk PRIMARY KEY (Chapter_ID),
    CONSTRAINT puzzles_chapter_Position_unique UNIQUE (Position)
);

CREATE TABLE puzzles
(
    Puzzle_ID INTEGER NOT NULL AUTO_INCREMENT,
    Class_ID INTEGER NOT NULL,
    Active_Role ENUM('ATTACKER', 'DEFENDER') NOT NULL,
    Level ENUM('EASY', 'HARD') DEFAULT 'HARD' NOT NULL,
    Max_Assertions INTEGER DEFAULT 2 NOT NULL ,
    Mutant_Validator_Level ENUM('STRICT', 'MODERATE', 'RELAXED') DEFAULT 'MODERATE' NOT NULL,
    Editable_Lines_Start INTEGER DEFAULT NULL,
    Editable_Lines_End INTEGER DEFAULT NULL,
    Chapter_ID INTEGER DEFAULT NULL, -- Chapter (group of puzzles) this puzzle belongs to
    Position INTEGER DEFAULT NULL, -- Index of the puzzle in the chapter
    Title VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    CONSTRAINT puzzles_pk PRIMARY KEY (Puzzle_ID),
    CONSTRAINT puzzles_classes_Class_ID_fk FOREIGN KEY (Class_ID) REFERENCES classes (Class_ID) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT puzzles_puzzle_chapters_Level_fk FOREIGN KEY (Chapter_ID) REFERENCES puzzle_chapters (Chapter_ID) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT puzzles_Chapter_ID_Position_unique UNIQUE (Chapter_ID, Position)
);

/* Add "Puzzle" column to classes. */
ALTER TABLE classes ADD Puzzle TINYINT(1) DEFAULT 0 NOT NULL;

/* Add "Puzzle_ID" column to games. */
ALTER TABLE games ADD Puzzle_ID INTEGER DEFAULT NULL;
ALTER TABLE games ADD CONSTRAINT games_puzzles_Puzzle_ID_fk FOREIGN KEY (Puzzle_ID) REFERENCES puzzles (Puzzle_ID) ON DELETE CASCADE ON UPDATE CASCADE;

/* Add "PUZZLE" mode to "Mode" column in games. */
ALTER TABLE games MODIFY COLUMN Mode ENUM('SINGLE','DUEL','PARTY','UTESTING', 'PUZZLE') NOT NULL DEFAULT 'DUEL';
