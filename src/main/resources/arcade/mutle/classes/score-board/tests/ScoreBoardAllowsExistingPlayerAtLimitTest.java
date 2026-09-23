import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ScoreBoardAllowsExistingPlayerAtLimitTest {
    @Test
    public void fullBoardStillAllowsAnExistingPlayer() {
        ScoreBoard board = new ScoreBoard(1);
        board.record("Ada", 5);

        assertTrue(board.record("Ada", 3));
        assertEquals(8, board.score("Ada"));
    }
}
