import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ScoreBoardStartsEmptyTest {
    @Test
    public void startsWithoutPlayers() {
        ScoreBoard board = new ScoreBoard(3);

        assertEquals(0, board.size());
        assertFalse(board.hasPlayer("Ada"));
    }
}
