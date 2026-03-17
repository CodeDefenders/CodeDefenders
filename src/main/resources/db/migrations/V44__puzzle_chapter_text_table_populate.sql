INSERT INTO puzzle_chapter_text (Chapter_ID, Language, Title, Description)
SELECT Chapter_ID, 'en', Title, Description FROM puzzle_chapters;
