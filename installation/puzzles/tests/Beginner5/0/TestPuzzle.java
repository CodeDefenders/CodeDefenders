import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test0() {
    Puzzle b = new Puzzle();
    assertEquals(3, b.run(3, 6));
  }

  @Test
  public void test1() {
    Puzzle b = new Puzzle();
    assertEquals(5, b.run(5, 6));
  }
}
