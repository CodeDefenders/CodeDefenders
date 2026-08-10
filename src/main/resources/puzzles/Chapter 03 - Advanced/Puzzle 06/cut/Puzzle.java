public class Puzzle {

    public int run(int x, int y) {
        if (x == y) {
            return x;
        } else if (x > y) {
            return run(x - y, y);
        } else {
            return run(x, y - x);
        }
    }

}
