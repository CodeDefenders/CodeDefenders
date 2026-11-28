import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestSleigh {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Sleigh("#S#S#")
        );

        assertEquals(
                "Multiple starting positions provided",
                exception.getMessage()
        );
    }
}
