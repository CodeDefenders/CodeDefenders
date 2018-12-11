CREATE TABLE `mutant_uploaded_with_class` (
  `Class_ID` int(11),
  `Mutant_ID`  int(11),
  PRIMARY KEY (`Class_ID`, `Mutant_ID`),
  FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`),
  FOREIGN KEY (`Mutant_ID`) REFERENCES mutants (`Mutant_ID`)
);

CREATE TABLE `test_uploaded_with_class` (
  `Class_ID` int(11),
  `Test_ID`  int(11),
  PRIMARY KEY (`Class_ID`, `Test_ID`),
  FOREIGN KEY (`Class_ID`) REFERENCES classes (`Class_ID`),
  FOREIGN KEY (`Test_ID`) REFERENCES tests (`Test_ID`)
);
