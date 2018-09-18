-- Dummy game for upload of tests and mutants together with a class

INSERT INTO games (ID, State)
VALUES (-1, 'FINISHED');

-- Automated attacker and defender
-- Dummy user for upload of tests and mutants together with a class
-- Dummy attacker (for mutants) and dummy defender (for tests)

INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`)
VALUES (-1, 'dummy_user', 'DUMMY_USER_INACCESSIBLE', 'user@dummy.com'),
       (-2, 'dummy_attacker', 'DUMMY_ATTACKER_INACCESSIBLE', 'attacker@dummy.com'),
       (-3, 'dummy_defender', 'DUMMY_DEFENDER_INACCESSIBLE', 'defender@dummy.com');


-- Dummy player for upload of tests and mutants together with a class
-- Dummy player attacker (for mutants) and dummy player defender (for tests)
-- All are added under the `users` table.

INSERT INTO `players` (`ID`, `User_ID`, `Game_ID`, `Points`, `Role`, `Active`)
VALUES (-1, -1, -1, 0, 'ATTACKER', 0),
       (-2, -2, -1, 0, 'ATTACKER', 0),
       (-3, -3, -1, 0, 'DEFENDER', 0);

-- Mutants uploaded together with a class can now reference the class
-- they were uploaded with

ALTER TABLE `mutants`
  ADD `Class_ID` int(11) DEFAULT NULL,
  ADD CONSTRAINT FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`);

-- Tests uploaded together with a class can now reference the class
-- they were uploaded with

ALTER TABLE `tests`
  ADD `Class_ID` int(11) DEFAULT NULL,
  ADD CONSTRAINT FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`);

