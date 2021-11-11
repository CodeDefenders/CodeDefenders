import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test() {
        Puzzle puzzle = new Puzzle();
        int result = puzzle.testMe(0, 0);
        assertEquals(4, result);
    }

}
