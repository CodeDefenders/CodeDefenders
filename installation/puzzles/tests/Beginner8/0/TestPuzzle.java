import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test() {
    Puzzle foo = new Puzzle();
    int result = foo.testMe(3, 4);
    assertEquals(0, result);
  }
}
