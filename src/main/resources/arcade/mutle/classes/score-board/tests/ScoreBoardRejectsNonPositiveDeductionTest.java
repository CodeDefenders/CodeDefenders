import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ScoreBoardRejectsNonPositiveDeductionTest {
    @Test
    public void nonPositiveDeductionsAreRejected() {
        ScoreBoard board = new ScoreBoard(2);
        board.record("Ada", 5);

        assertFalse(board.deduct("Ada", 0));
        assertFalse(board.deduct("Ada", -1));
    }
}
