import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test0() {
    Puzzle b = new Puzzle();
    assertEquals(1, b.run(3, 3));
  }

  @Test
  public void test1() {
    Puzzle b = new Puzzle();
    assertEquals(1, b.run(5, 3));
  }

  @Test
  public void test2() {
    Puzzle b = new Puzzle();
    assertEquals(1, b.run(3, 7));
  }
}
