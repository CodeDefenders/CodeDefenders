import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test() {
    Puzzle foo = new Puzzle();
    int result = foo.testMe(2, 0);
    assertEquals(1, result);
  }
}
