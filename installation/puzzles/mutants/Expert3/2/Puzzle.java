public class Puzzle {
  public int run(int x) {
    int y = 10;
    int z = x;
    while (z % y > 0) {
      y = y - 1;
    }
    return y;
  }
}
