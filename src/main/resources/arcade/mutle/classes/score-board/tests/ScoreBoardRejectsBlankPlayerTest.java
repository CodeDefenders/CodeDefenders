import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class ScoreBoardRejectsBlankPlayerTest {
    @Test
    public void blankPlayersAreInvalid() {
        ScoreBoard board = new ScoreBoard(2);

        assertThrows(IllegalArgumentException.class, () -> board.record(" ", 1));
    }
}
