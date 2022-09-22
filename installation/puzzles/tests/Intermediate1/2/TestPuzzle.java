import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test() {
        Puzzle b = new Puzzle();
        assertEquals(1, b.run(3, 7));
    }

}
