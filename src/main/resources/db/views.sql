CREATE OR REPLACE VIEW `vw_mp_tests`
AS
SELECT
  u.User_ID,
  t.Test_ID

FROM users u
  JOIN players p
    ON u.User_ID = p.User_ID
  JOIN tests t
    ON p.ID = t.Player_ID
  JOIN games g
    ON t.Game_ID = g.ID

WHERE
  g.Mode = 'PARTY'
;

CREATE OR REPLACE VIEW `vw_mp_tests_kill_mutants`
AS
SELECT
  u.User_ID,
  t.Test_ID

FROM users u
  JOIN players p
    ON u.User_ID = p.User_ID
  JOIN tests t
    ON p.ID = t.Player_ID
  JOIN games g
    ON t.Game_ID = g.ID
  JOIN targetexecutions trg
    ON t.Test_ID = trg.Test_ID

WHERE
  g.Mode = 'PARTY' AND
  trg.Target = 'TEST_MUTANT' AND
  (trg.Status = 'FAIL' OR trg.Status = 'ERROR')
;

CREATE OR REPLACE VIEW `vw_mp_mutants`
AS
SELECT
  u.User_ID,
  m.Mutant_ID

FROM users u
  JOIN players p
    ON u.User_ID = p.User_ID
  JOIN mutants m
    on p.ID = m.Player_ID
  JOIN games g
    on m.Game_ID = g.ID

WHERE
  g.Mode = 'PARTY'
;