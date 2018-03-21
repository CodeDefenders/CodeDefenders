ALTER TABLE equivalences DROP FOREIGN KEY fk_equiv_def;
ALTER TABLE equivalences
  ADD CONSTRAINT fk_equiv_def
FOREIGN KEY (Defender_ID) REFERENCES players (ID) ON DELETE CASCADE;

ALTER TABLE equivalences DROP FOREIGN KEY fk_equiv_mutant;
ALTER TABLE equivalences
  ADD CONSTRAINT fk_equiv_mutant
FOREIGN KEY (Mutant_ID) REFERENCES mutants (Mutant_ID) ON DELETE CASCADE;

ALTER TABLE games DROP FOREIGN KEY fk_classId;
ALTER TABLE games
  ADD CONSTRAINT fk_classId
FOREIGN KEY (Class_ID) REFERENCES classes (Class_ID) ON DELETE CASCADE;

ALTER TABLE games DROP FOREIGN KEY fk_className;
ALTER TABLE games
  ADD CONSTRAINT fk_className
FOREIGN KEY (Class_ID) REFERENCES classes (Class_ID) ON DELETE CASCADE;

ALTER TABLE games DROP FOREIGN KEY fk_creatorId;
ALTER TABLE games
  ADD CONSTRAINT fk_creatorId
FOREIGN KEY (Creator_ID) REFERENCES users (User_ID) ON DELETE CASCADE;

ALTER TABLE mutants DROP FOREIGN KEY fk_gameId_muts;
ALTER TABLE mutants
  ADD CONSTRAINT fk_gameId_muts
FOREIGN KEY (Game_ID) REFERENCES games (ID) ON DELETE CASCADE;

ALTER TABLE mutants DROP FOREIGN KEY fk_playerId_muts;
ALTER TABLE mutants
  ADD CONSTRAINT fk_playerId_muts
FOREIGN KEY (Player_ID) REFERENCES players (ID) ON DELETE CASCADE;

ALTER TABLE players DROP FOREIGN KEY fk_gameId_players;
ALTER TABLE players
  ADD CONSTRAINT fk_gameId_players
FOREIGN KEY (Game_ID) REFERENCES games (ID) ON DELETE CASCADE;

ALTER TABLE players DROP FOREIGN KEY fk_userId_players;
ALTER TABLE players
  ADD CONSTRAINT fk_userId_players
FOREIGN KEY (User_ID) REFERENCES users (User_ID) ON DELETE CASCADE;

ALTER TABLE sessions DROP FOREIGN KEY fk_userId_sessions;
ALTER TABLE sessions
  ADD CONSTRAINT fk_userId_sessions
FOREIGN KEY (User_ID) REFERENCES users (User_ID) ON DELETE CASCADE;

ALTER TABLE targetexecutions DROP FOREIGN KEY targetexecutions_ibfk_1;
ALTER TABLE targetexecutions
  ADD CONSTRAINT targetexecutions_ibfk_1
FOREIGN KEY (Test_ID) REFERENCES tests (Test_ID) ON DELETE CASCADE;

ALTER TABLE targetexecutions DROP FOREIGN KEY targetexecutions_ibfk_2;
ALTER TABLE targetexecutions
  ADD CONSTRAINT targetexecutions_ibfk_2
FOREIGN KEY (Mutant_ID) REFERENCES mutants (Mutant_ID) ON DELETE CASCADE;

ALTER TABLE tests DROP FOREIGN KEY fk_gameId_tests;
ALTER TABLE tests
  ADD CONSTRAINT fk_gameId_tests
FOREIGN KEY (Game_ID) REFERENCES games (ID) ON DELETE CASCADE;

ALTER TABLE tests DROP FOREIGN KEY fk_playerId_tests;
ALTER TABLE tests
  ADD CONSTRAINT fk_playerId_tests
FOREIGN KEY (Player_ID) REFERENCES players (ID) ON DELETE CASCADE;

ALTER TABLE usedaimutants DROP FOREIGN KEY fk_gameId_ai_mutants;
ALTER TABLE usedaimutants
  ADD CONSTRAINT fk_gameId_ai_mutants
FOREIGN KEY (Game_ID) REFERENCES games (ID) ON DELETE CASCADE;

ALTER TABLE usedaitests DROP FOREIGN KEY fk_gameId_ai_test;
ALTER TABLE usedaitests
  ADD CONSTRAINT fk_gameId_ai_test
FOREIGN KEY (Game_ID) REFERENCES games (ID) ON DELETE CASCADE;