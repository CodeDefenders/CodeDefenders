INSERT INTO achievements
VALUES (10, 0, 10, 'No smell free tests written', 'Write a test without test smells to unlock this achievement',
        '{0} of {1} smell free tests written', 0),
       (10, 1, 10, 'This smells good', 'Write the first test without smells',
        '{0} of {1} smell free tests written to reach the next level', 1),
       (10, 2, 10, 'Bronze Perfumer', 'Write {0} smell free tests',
        '{0} of {1} smell free tests written to reach the next level', 10),
       (10, 3, 10, 'Silver Perfumer', 'Write {0} smell free tests',
        '{0} of {1} smell free tests written to reach the next level', 25),
       (10, 4, 10, 'Perfume Expert', 'Write {0} smell free tests', '{0} smell free tests written, max level reached',
        100),

       (11, 0, 11, 'No mutants killed', 'Kill a mutant to unlock this achievement', '{0} of {1} mutant killed', 0),
       (11, 1, 11, 'Get the mutants!', 'Kill the first mutant with a test',
        '{0} of {1} mutants killed to reach the next level', 1),
       (11, 2, 11, 'Bronze Mutant Killer', 'Kill {0} mutants with tests',
        '{0} of {1} mutants killed to reach the next level', 10),
       (11, 3, 11, 'Silver Mutant Killer', 'Kill {0} mutants with tests',
        '{0} of {1} mutants killed to reach the next level', 50),
       (11, 4, 11, 'Mutant Executioner', 'Kill {0} mutants with tests', '{0} mutants killed, max level reached', 200),

       (12, 0, 12, 'No lines covered', 'Cover at least one line of code using tests to unlock this achievement',
        '{0} of {1} lines covered', 0),
       (12, 1, 12, 'Cover the first line', 'Cover the first line of code with a test',
        '{0} of {1} lines covered to reach the next level', 1),
       (12, 2, 12, 'Bronze Coverer', 'Cover {0} lines of code with tests',
        '{0} of {1} lines covered to reach the next level', 100),
       (12, 3, 12, 'Silver Coverer', 'Cover {0} lines of code with tests',
        '{0} of {1} lines covered to reach the next level', 500),
       (12, 4, 12, 'Line Coverage Expert', 'Cover {0} lines of code with tests', '{0} lines covered, max level reached',
        1000),

       (13, 0, 13, 'No tests with coverage',
        'Write a test that covers at least one line of code to unlock this achievement',
        '{0} of {1} lines covered', 0),
       (13, 1, 13, 'Specific Tester', 'Write a test that covers at least one line of code',
        '{0} of {1} lines covered to reach the next level', 1),
       (13, 2, 13, 'Small Tester', 'Cover {0} lines of code using a single test',
        '{0} of {1} lines covered to reach the next level', 10),
       (13, 3, 13, 'Large-Scale Tester', 'Cover {0} lines of code using a single test',
        '{0} of {1} lines covered to reach the next level', 25),
       (13, 4, 13, 'All-Encompassing Tester', 'Cover {0} lines of code using a single test',
        'You wrote a test that covered {0} lines, this is crazy, please stop :o', 100),

       (14, 0, 14, 'No Duels won', 'Win your first equivalence duel to unlock this achievement',
        '{0} of {1} equivalence duels won', 0),
       (14, 1, 14, 'Beginner Duelist', 'Win your first equivalence duel',
        '{0} of {1} equivalence duels won to reach the next level', 1),
       (14, 2, 14, 'Bronze Duelist', 'Win {0} equivalence duels',
        '{0} of {1} equivalence duels won to reach the next level', 5),
       (14, 3, 14, 'Silver Duelist', 'Win {0} equivalence duels',
        '{0} of {1} equivalence duels won to reach the next level', 10),
       (14, 4, 14, 'The Best Duelist', 'Win {0} equivalence duels', '{0} equivalence duels won, max level reached',
        50);
