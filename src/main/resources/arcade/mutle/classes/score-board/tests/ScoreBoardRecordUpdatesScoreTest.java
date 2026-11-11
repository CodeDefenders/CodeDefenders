import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreBoardRecordUpdatesScoreTest {
    @Test
    public void recordingUpdatesTheScore() {
        ScoreBoard board = new ScoreBoard(2);

        board.record("Ada", 5);

        assertEquals(5, board.score("Ada"));
    }
}
