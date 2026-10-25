import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test2() {
        Puzzle b = new Puzzle();
        assertEquals(2, b.run(1, 1));
    }

}
