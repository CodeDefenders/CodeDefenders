public class Puzzle {

    public int testMe(int x, int y) {
        int z = 0;

        while (x > y) {
            if (x % 2 == 0) {
                z = z + 1;
            }
            x--;
        }

        return z;
    }

}
