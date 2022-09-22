import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test() {
        Puzzle b = new Puzzle();
        assertEquals(4, b.run(4, 7));
    }

}
