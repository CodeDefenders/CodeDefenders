/* Add "EquivalenceThreshold" column to games. */
ALTER TABLE mutants ADD `KillMessage` varchar(2000) DEFAULT NULL;

/* Update mutants views */
CREATE OR REPLACE VIEW `view_mutants_with_user` AS
SELECT mutants.*, users.*
FROM mutants
       LEFT JOIN players ON players.ID = mutants.Player_ID
       LEFT JOIN users ON players.User_ID = users.User_ID;

CREATE OR REPLACE VIEW `view_valid_mutants` AS
SELECT *
FROM view_mutants_with_user
WHERE ClassFile IS NOT NULL;
