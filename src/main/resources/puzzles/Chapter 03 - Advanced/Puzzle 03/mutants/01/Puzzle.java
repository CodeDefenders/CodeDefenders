public class Puzzle {

    public int run(int x, int y) {
        int z = 0;

        while (x >= 0) {
            z += x + y;
            x--;
        }

        return z;
    }

}
