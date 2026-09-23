import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class ScoreBoardRejectsLimitOutsideRangeTest {
    @Test
    public void rejectsLimitsOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> new ScoreBoard(0));
        assertThrows(IllegalArgumentException.class, () -> new ScoreBoard(11));
    }
}
