public class Puzzle {

    public int run(int x, int y) {
        int z = 0;

        if (x % 2 == 0) {
            if (y % 4 == 0) {
                z = z + 1;
            }
            z = z + 1;
        }
        if (z == 0 || x == y) {
            z = -1;
        }

        return z;
    }

}
