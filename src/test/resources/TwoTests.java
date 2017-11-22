import org.junit.*;

import static org.junit.Assert.*;

public class TwoTests {
    @Test(timeout = 4000)
    public void test1() throws Throwable {
        assertTrue(true);
    }

    @Test(timeout = 4000)
    public void test2() throws Throwable {
        assertTrue(true);
    }
}
