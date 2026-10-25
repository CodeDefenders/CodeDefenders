public class Puzzle {

    public int run(int x, int y) {
        int z = -1;

        if (y > x) {
            z = 10;
        }
        if (x > y) {
            z = 1;
        }

        if (z > x) {
            return z;
        } else {
            return x;
        }
    }

}
