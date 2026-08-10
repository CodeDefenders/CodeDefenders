import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test1() {
        Puzzle b = new Puzzle();
        assertEquals(5, b.run(5, 0));
    }

}
