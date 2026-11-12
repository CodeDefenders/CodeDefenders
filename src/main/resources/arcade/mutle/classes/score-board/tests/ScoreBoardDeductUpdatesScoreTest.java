import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreBoardDeductUpdatesScoreTest {
    @Test
    public void deductingUpdatesTheScore() {
        ScoreBoard board = new ScoreBoard(2);
        board.record("Ada", 5);

        board.deduct("Ada", 2);

        assertEquals(3, board.score("Ada"));
    }
}
