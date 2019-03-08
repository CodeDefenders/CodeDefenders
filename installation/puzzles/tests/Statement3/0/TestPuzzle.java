import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {
  @Test
  public void test0() {
    Puzzle s = new Puzzle();
    try {
      s.run(0, 0);
      fail("Expected exception");
    } catch(Throwable t) {
      // OK
    }
  }
}
