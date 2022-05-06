public class Puzzle {

    public int run(int x) {
        int y = 9;
        int z = 10 * x;
        while (z % y > 0) {
            y = y - 1;
        }
        return y;
    }

}
