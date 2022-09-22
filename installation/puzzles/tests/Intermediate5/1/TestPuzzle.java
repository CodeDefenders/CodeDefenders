import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test() {
        Puzzle b = new Puzzle();
        assertEquals(2, b.run(8, 16));
    }

}
