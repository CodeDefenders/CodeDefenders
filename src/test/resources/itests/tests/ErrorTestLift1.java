/* no package name */

import org.junit.*;

import static org.junit.Assert.*;

public class TestLift {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        Elevator e = new Elevator(10, 2);
        e.addRiders(1);
        assertEquals(1, e.getNumRiders());
        assertFalse(e.isFull());
    }
}