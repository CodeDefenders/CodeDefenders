ALTER TABLE puzzles
    ADD COLUMN `IsEquivalent` BOOLEAN DEFAULT FALSE;
ALTER TABLE puzzles
    ADD COLUMN `IsEquivalencePuzzle` BOOLEAN DEFAULT FALSE;
