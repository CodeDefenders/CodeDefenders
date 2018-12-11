/* no package name */

import org.junit.*;
import static org.junit.Assert.*;

public class TestLift {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        Lift l = new Lift(5);
        l.getTopFloor(); // This cover the mutant
        assertEquals(0, l.getCurrentFloor());
    }
}