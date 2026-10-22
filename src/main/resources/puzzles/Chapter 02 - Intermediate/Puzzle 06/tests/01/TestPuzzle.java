import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test(timeout = 4000)
    public void test() {
        Puzzle foo = new Puzzle();
        String result = foo.run(7);
        assertEquals("OOOOOOO", result);
    }

}
