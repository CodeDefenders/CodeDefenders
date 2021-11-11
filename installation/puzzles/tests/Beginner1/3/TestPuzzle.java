import org.junit.*;

import static org.junit.Assert.*;

public class TestPuzzle {

    @Test
    public void test() {
        Puzzle s = new Puzzle();
        assertEquals(-1, s.bar(0));
    }

}
