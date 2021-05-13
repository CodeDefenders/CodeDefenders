public class Puzzle {
  public int run(int x, int y) {
    while (x >= 10 || y < 0) {
      x = x - 1;
      y = - y;
    }
    return x * y;
  }
}
