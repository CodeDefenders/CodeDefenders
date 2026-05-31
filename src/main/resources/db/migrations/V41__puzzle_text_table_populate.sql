INSERT INTO puzzle_text (Puzzle_ID, Language, Title, Description)
SELECT Puzzle_ID, 'en', Title, Description FROM puzzles;
