import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreBoardRepeatedRecordsAccumulateTest {
    @Test
    public void repeatedRecordsAccumulate() {
        ScoreBoard board = new ScoreBoard(2);

        board.record("Ada", 5);
        board.record("Ada", 3);

        assertEquals(8, board.score("Ada"));
    }
}
