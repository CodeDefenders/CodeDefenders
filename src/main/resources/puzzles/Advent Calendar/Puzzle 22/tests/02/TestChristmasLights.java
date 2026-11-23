import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestChristmasLights {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        var lights = new ChristmasLights(5);
        lights.switchOn();
        lights.switchOff();
        assertEquals(0, lights.countLightsOn());
    }
}
