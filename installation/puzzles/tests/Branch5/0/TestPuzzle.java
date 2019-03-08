import org.junit.*;
import static org.junit.Assert.*;

public class TestPuzzle {

  @Test
  public void test0() {
    Puzzle b = new Puzzle();
    assertEquals(0, b.run(0, 0));
  }

  @Test
  public void test1() {
    Puzzle b = new Puzzle();
    assertEquals(5, b.run(5, 0));
  }

  @Test
  public void test2() {
    Puzzle b = new Puzzle();
    assertEquals(2, b.run(1, 1));
  }

  @Test
  public void test3() {
    Puzzle b = new Puzzle();
    assertEquals(5, b.run(5, 1));
  }

}
