import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test() {
    Puzzle foo = new Puzzle();
    int result = foo.testMe(42, 1);
    assertEquals(1, result);
  }
}
