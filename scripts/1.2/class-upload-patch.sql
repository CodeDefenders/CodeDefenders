-- Dummy game for upload of tests and mutants together with a class

INSERT INTO games (ID, State)
VALUES (-1, 'FINISHED');

-- Mapping between mutants and the class the mutants are mutated from
--

DROP TABLE IF EXISTS `mutant_belongs_to_class`;
CREATE TABLE `mutant_belongs_to_class` (
  `Class_ID`  int(11),
  `Mutant_ID` int(11),
  PRIMARY KEY (`Class_ID`, `Mutant_ID`),
  FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`),
  FOREIGN KEY (`Mutant_ID`) REFERENCES mutants (`Mutant_ID`)
)
  ENGINE = InnoDB
  CHARSET = utf8;

--
-- Dummy player for upload of tests and mutants together with a class
-- Dummy player attacker (for mutants) and dummy player defender (for tests)
-- All are added under the `users` table.
--

INSERT INTO `players` (`ID`, `User_ID`, `Game_ID`)
VALUES (-1, -1, -1),
       (-2, -2, -1),
       (-3, -3, -1);

--
-- Mapping between tests and the class the test are created for
--

DROP TABLE IF EXISTS `test_belongs_to_class`;
CREATE TABLE `test_belongs_to_class` (
  `Class_ID` int(11),
  `Test_ID`  int(11),
  PRIMARY KEY (`Class_ID`, `Test_ID`),
  FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`),
  FOREIGN KEY (`Test_ID`) REFERENCES tests (`Test_ID`)
)
  ENGINE = InnoDB
  CHARSET = utf8;

--
-- Automated attacker and defender
-- Dummy user for upload of tests and mutants together with a class
-- Dummy attacker (for mutants) and dummy defender (for tests)
--

INSERT INTO `users` (`User_ID`, `Username`, `Password`, `Email`)
VALUES (-1, 'dummy_user', 'DUMMY_USER_INACCESSIBLE', 'user@dummy.com'),
       (-2, 'dummy_attacker', 'DUMMY_ATTACKER_INACCESSIBLE', 'attacker@dummy.com'),
       (-3, 'dummy_defender', 'DUMMY_DEFENDER_INACCESSIBLE', 'defender@dummy.com');

