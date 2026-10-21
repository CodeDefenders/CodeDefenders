public class Puzzle {

    public int run(int x) {
        int y = 9;
        int z = x;
        while (z % y > 1) {
            y = y - 1;
        }
        return y;
    }

}
