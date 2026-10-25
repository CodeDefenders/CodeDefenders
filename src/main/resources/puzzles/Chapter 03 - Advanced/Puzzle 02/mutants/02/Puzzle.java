public class Puzzle {

    public int run(int x, int y, int z) {
        while (x > y && x > 0) {
            if (z % x == 1) {
                return x - 1;
            }
            x = x - 1;
        }
        return 0;
    }

}
