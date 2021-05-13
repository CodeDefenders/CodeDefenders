public class Puzzle {
  
  public int testMe(int x, int y) {

    int a = 0;
    int b = 0;

    if(x == 42) {
      a = 1;
      b = y;
    }

    if(y == 100) {
      b = a - x;
    }

    return b;
  }
}
