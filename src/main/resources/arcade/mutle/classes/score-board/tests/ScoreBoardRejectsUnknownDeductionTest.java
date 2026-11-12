import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ScoreBoardRejectsUnknownDeductionTest {
    @Test
    public void unknownPlayersCannotLosePoints() {
        assertFalse(new ScoreBoard(2).deduct("Ada", 1));
    }
}
