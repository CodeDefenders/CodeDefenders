import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test1() {
    Puzzle b = new Puzzle();
    assertEquals(2, b.run(3, 4, 3));
  }
}
