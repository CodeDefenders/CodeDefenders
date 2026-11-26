import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestSleigh {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        Sleigh sleigh = new Sleigh(
                ".#H.",
                "~..S",
                "H..."
        );

        assertEquals("(1,3)", sleigh.getPosition());
        assertEquals(0, sleigh.deliveredHouses());
    }
}
