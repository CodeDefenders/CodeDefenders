import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test1() {
        Puzzle b = new Puzzle();
        assertEquals(5, b.run(5, 0));
    }

}
