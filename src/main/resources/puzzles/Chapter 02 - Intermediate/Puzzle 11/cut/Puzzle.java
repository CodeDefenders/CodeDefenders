public class Puzzle {

    public int testMe(int x, int y) {
        int a = 1;
        int b = 1;

        if (x == 42) {
            a = a + 1;
            y = y * 2;
        }

        if (y == 100) {
            b = 2 * a;
        }

        return a * b;
    }

}
