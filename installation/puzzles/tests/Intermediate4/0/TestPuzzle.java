import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test() {
        Puzzle b = new Puzzle();
        assertEquals(7, b.run(5, 7));
    }

}
