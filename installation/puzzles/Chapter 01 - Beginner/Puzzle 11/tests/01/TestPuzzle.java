import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test() {
        Puzzle puzzle = new Puzzle();
        int result = puzzle.testMe(42, 0);
        assertEquals(5, result);
    }

}
