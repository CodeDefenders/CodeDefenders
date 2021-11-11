import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test3() {
        Puzzle b = new Puzzle();
        assertEquals(-1, b.run(1, 2));
    }

}
