import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreBoardAddsDifferentPlayersTest {
    @Test
    public void differentPlayersOccupyDifferentEntries() {
        ScoreBoard board = new ScoreBoard(2);

        board.record("Ada", 5);
        board.record("Lin", 3);

        assertEquals(2, board.size());
        assertEquals(3, board.score("Lin"));
    }
}
