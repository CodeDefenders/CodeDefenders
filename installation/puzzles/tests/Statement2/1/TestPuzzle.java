import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {
  @Test
  public void test0() {
    Puzzle s = new Puzzle();
    assertEquals(24, s.run(3));
  }
}
