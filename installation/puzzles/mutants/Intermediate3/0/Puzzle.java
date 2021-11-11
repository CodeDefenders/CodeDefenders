public class Puzzle {

    public int run(int x, int y) {
        int z = 0;

        if (x == 5) {
            if (y == 7) {
                z = y;
            } else {
                z = 0;
            }
        } else {
            z = x + y;
        }

        return z;
    }

}
