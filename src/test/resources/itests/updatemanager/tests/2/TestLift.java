
/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestLift {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        // test here!
        Lift l = new Lift(5, 10);
        l.addRiders(1);
        assertFalse(l.isFull());
        assertEquals(1, l.getNumRiders());

    }
}
