public class Puzzle {

    public int run(int x, int y) {
        if (x == y) {
            return x;
        } else if (x > y) {
            return run(y, x - y);
        } else {
            return run(x, y - x);
        }
    }

}
