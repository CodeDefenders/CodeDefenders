public class Puzzle {

    public int run(int x, int y) {
        int z = 0;

        if (x < y + 1) {
            z++;

            if (y != x) {
                z++;
            }
        }

        return z;
    }

}
