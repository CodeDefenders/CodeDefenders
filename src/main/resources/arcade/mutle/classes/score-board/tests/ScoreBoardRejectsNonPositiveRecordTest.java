import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ScoreBoardRejectsNonPositiveRecordTest {
    @Test
    public void nonPositiveRecordsAreRejected() {
        ScoreBoard board = new ScoreBoard(2);

        assertFalse(board.record("Ada", 0));
        assertFalse(board.record("Ada", -1));
        assertEquals(0, board.size());
    }
}
