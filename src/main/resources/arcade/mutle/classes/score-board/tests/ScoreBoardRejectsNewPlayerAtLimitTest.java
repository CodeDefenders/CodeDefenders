import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ScoreBoardRejectsNewPlayerAtLimitTest {
    @Test
    public void fullBoardRejectsNewPlayers() {
        ScoreBoard board = new ScoreBoard(1);
        board.record("Ada", 5);

        assertFalse(board.record("Lin", 3));
        assertEquals(1, board.size());
    }
}
