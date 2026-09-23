import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ScoreBoardRejectsExcessDeductionTest {
    @Test
    public void deductionCannotExceedCurrentScore() {
        ScoreBoard board = new ScoreBoard(2);
        board.record("Ada", 5);

        assertFalse(board.deduct("Ada", 6));
        assertEquals(5, board.score("Ada"));
    }
}
