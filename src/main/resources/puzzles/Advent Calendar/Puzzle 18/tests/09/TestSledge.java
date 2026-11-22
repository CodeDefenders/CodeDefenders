import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestSledge {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        Sledge sledge = new Sledge("HSHH");

        sledge.move('>');
        sledge.move('>');
        sledge.move('<');

        assertEquals("(0,2)", sledge.getPosition());
        assertEquals(2, sledge.deliveredHouses());
    }
}
