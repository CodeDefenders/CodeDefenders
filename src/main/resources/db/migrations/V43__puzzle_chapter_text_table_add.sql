CREATE TABLE puzzle_chapter_text (
    Chapter_ID  INT(11) NOT NULL,
    Language    VARCHAR(3) NOT NULL,
    Title       VARCHAR(100) DEFAULT NULL,
    Description VARCHAR(1000) DEFAULT NULL,
    PRIMARY KEY (Chapter_ID, Language),
    FOREIGN KEY (Chapter_ID) REFERENCES puzzle_chapters(Chapter_ID) ON DELETE CASCADE
);
