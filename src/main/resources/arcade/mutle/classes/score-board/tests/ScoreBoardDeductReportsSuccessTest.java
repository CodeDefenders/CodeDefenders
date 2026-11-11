import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ScoreBoardDeductReportsSuccessTest {
    @Test
    public void deductingReportsSuccess() {
        ScoreBoard board = new ScoreBoard(2);
        board.record("Ada", 5);

        assertTrue(board.deduct("Ada", 2));
    }
}
