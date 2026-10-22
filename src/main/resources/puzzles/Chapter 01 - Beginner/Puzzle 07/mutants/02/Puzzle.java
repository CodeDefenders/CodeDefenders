public class Puzzle {

    public int makeNegative(int x) {
        return min(x, -x);
    }

    private int min(int x, int y) {
        if (x < y) {
            return y;
        }
        return x;
    }

}
