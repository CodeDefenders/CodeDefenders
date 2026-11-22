import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestSledge {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        Sledge sledge = new Sledge(
                ".#H.",
                "~..S",
                "H..."
        );

        assertTrue(sledge.isInBounds(1, 2));
    }
}
