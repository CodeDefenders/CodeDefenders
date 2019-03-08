import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test0() {
    Puzzle b = new Puzzle();
    assertEquals(8, b.run(-1));
  }

  @Test
  public void test1() {
    Puzzle b = new Puzzle();
    assertEquals(8, b.run(0));
  }

  @Test
  public void test2() {
    Puzzle b = new Puzzle();
    assertEquals(8, b.run(1));
  }

  @Test
  public void test3() {
    Puzzle b = new Puzzle();
    assertEquals(8, b.run(4));
  }
}
