import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestSleigh {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        Sleigh sleigh = new Sleigh("SH.H");

        sleigh.move('>');
        assertEquals(1, sleigh.deliveredHouses());
    }
}
