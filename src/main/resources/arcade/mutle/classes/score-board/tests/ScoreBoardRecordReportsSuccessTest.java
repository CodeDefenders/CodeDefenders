import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ScoreBoardRecordReportsSuccessTest {
    @Test
    public void recordingReportsSuccess() {
        assertTrue(new ScoreBoard(2).record("Ada", 5));
    }
}
