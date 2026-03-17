CREATE TABLE puzzle_text (
    Puzzle_ID   INT(11) NOT NULL,
    Language    VARCHAR(3) NOT NULL,
    Title       VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    PRIMARY KEY (Puzzle_ID, Language),
    FOREIGN KEY (Puzzle_ID) REFERENCES puzzles(Puzzle_ID) ON DELETE CASCADE
);
