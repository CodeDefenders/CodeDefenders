import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test() {
    Puzzle foo = new Puzzle();
    int result = foo.testMe(0, 0);
    assertEquals(0, result);
  }
}
