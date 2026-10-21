import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test() {
        Puzzle foo = new Puzzle();
        int result = foo.run(4, 0);
        assertEquals(10, result);
    }

}
