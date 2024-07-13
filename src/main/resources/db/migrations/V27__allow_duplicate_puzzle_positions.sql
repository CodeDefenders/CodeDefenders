-- puzzles_Chapter_ID_Position_unique is "needed in a foreign key constraint",
-- so we drop the FK and add it back later.
ALTER TABLE puzzles
    DROP FOREIGN KEY puzzles_puzzle_chapters_Level_fk;

-- Drop unique constraints for puzzle and chapter positions.
ALTER TABLE puzzles
    DROP KEY puzzles_Chapter_ID_Position_unique;
ALTER TABLE puzzle_chapters
    DROP KEY puzzles_chapter_Position_unique;

-- Add the foreign key back.
ALTER TABLE puzzles
    ADD FOREIGN KEY puzzles_puzzle_chapters_Level_fk (Chapter_ID)
        REFERENCES puzzle_chapters (Chapter_ID) ON DELETE SET NULL ON UPDATE CASCADE;
