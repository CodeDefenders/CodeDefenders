import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test0() {
        Puzzle b = new Puzzle();
        assertEquals(0, b.run(5, 0));
    }

    @Test
    public void test1() {
        Puzzle b = new Puzzle();
        assertEquals(1, b.run(3, 0));
    }

    @Test
    public void test2() {
        Puzzle b = new Puzzle();
        assertEquals(0, b.run(5, 7));
    }

}
