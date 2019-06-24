/* Add "Active" column to classes. */
ALTER TABLE classes ADD Active TINYINT(1) DEFAULT 1 NOT NULL;

/* Update views */
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
