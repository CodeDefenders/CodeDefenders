public class Puzzle {

    public int run(int x, int y, int z) {
        while (x > y && x > 0) {
            if (z % x == 0) {
                return x;
            }
            x = x - 1;
        }
        return -1;
    }

}
