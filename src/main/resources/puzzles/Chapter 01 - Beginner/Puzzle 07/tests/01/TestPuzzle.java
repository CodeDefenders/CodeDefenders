import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test() {
        Puzzle s = new Puzzle();
        assertEquals(-2, s.makeNegative(2));
    }

}
