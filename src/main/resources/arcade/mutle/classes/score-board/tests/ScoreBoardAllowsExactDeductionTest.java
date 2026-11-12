import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScoreBoardAllowsExactDeductionTest {
    @Test
    public void exactDeductionLeavesZero() {
        ScoreBoard board = new ScoreBoard(2);
        board.record("Ada", 5);

        assertTrue(board.deduct("Ada", 5));
        assertEquals(0, board.score("Ada"));
    }
}
