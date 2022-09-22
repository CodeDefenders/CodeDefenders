import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test0() {
        Puzzle b = new Puzzle();
        assertEquals(0, b.run(0, 0));
    }

}
