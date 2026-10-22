import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test() {
        Puzzle b = new Puzzle();
        assertEquals(3, b.run(3, 6));
    }

}
